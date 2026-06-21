package com.resumeanalyzer.interview.service;

import com.resumeanalyzer.ai.AiProviderResolver;
import com.resumeanalyzer.ai.model.AnswerEvaluation;
import com.resumeanalyzer.ai.model.ChatTurn;
import com.resumeanalyzer.ai.model.InterviewFeedback;
import com.resumeanalyzer.ai.model.InterviewQuestion;
import com.resumeanalyzer.common.dto.PageResponse;
import com.resumeanalyzer.common.exception.BadRequestException;
import com.resumeanalyzer.common.exception.ResourceNotFoundException;
import com.resumeanalyzer.interview.domain.InterviewMessage;
import com.resumeanalyzer.interview.domain.InterviewSession;
import com.resumeanalyzer.interview.domain.InterviewStatus;
import com.resumeanalyzer.interview.domain.MessageSender;
import com.resumeanalyzer.interview.dto.GenerateQuestionsRequest;
import com.resumeanalyzer.interview.dto.InterviewSessionDto;
import com.resumeanalyzer.interview.dto.SendMessageRequest;
import com.resumeanalyzer.interview.dto.StartInterviewRequest;
import com.resumeanalyzer.interview.mapper.InterviewMapper;
import com.resumeanalyzer.interview.repository.InterviewSessionRepository;
import com.resumeanalyzer.job.domain.JobDescription;
import com.resumeanalyzer.job.service.JobService;
import com.resumeanalyzer.resume.service.ResumeService;
import com.resumeanalyzer.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the interview features: tailored question generation, the turn-by-turn mock
 * interview chat, and final scoring. All AI interactions go through the configured provider.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final AiProviderResolver aiProviderResolver;
    private final ResumeService resumeService;
    private final JobService jobService;
    private final UserService userService;
    private final InterviewMapper interviewMapper;

    @Transactional(readOnly = true)
    public List<InterviewQuestion> generateQuestions(UUID userId, GenerateQuestionsRequest request) {
        String resumeText = resolveResumeText(userId, request.resumeId());
        String jobText = resolveJobText(userId, request.jobDescriptionId());
        return aiProviderResolver.current().generateQuestions(resumeText, jobText, request.effectiveCount());
    }

    @Transactional
    public InterviewSessionDto start(UUID userId, StartInterviewRequest request) {
        String resumeText = resolveResumeText(userId, request.resumeId());
        String jobText = resolveJobText(userId, request.jobDescriptionId());
        JobDescription jd = request.jobDescriptionId() == null ? null
                : jobService.getOwnedJob(userId, request.jobDescriptionId());

        InterviewSession session = InterviewSession.builder()
                .user(userService.getUser(userId))
                .jobDescription(jd)
                .status(InterviewStatus.IN_PROGRESS)
                .build();

        String opening = aiProviderResolver.current().startInterview(resumeText, jobText);
        session.addMessage(InterviewMessage.builder()
                .sender(MessageSender.AI)
                .message(opening)
                .build());

        sessionRepository.save(session);
        return interviewMapper.toDto(session);
    }

    @Transactional
    public InterviewSessionDto sendMessage(UUID userId, SendMessageRequest request) {
        InterviewSession session = sessionRepository
                .findWithMessagesByIdAndUserId(request.sessionId(), userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Interview session", request.sessionId()));
        if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BadRequestException("This interview session is already " + session.getStatus());
        }

        List<ChatTurn> conversation = toConversation(session);

        InterviewMessage userMessage = InterviewMessage.builder()
                .sender(MessageSender.USER)
                .message(request.message())
                .build();
        session.addMessage(userMessage);

        AnswerEvaluation evaluation = aiProviderResolver.current().evaluateAnswer(conversation, request.message());
        userMessage.setAnswerScore(evaluation.score());

        session.addMessage(InterviewMessage.builder()
                .sender(MessageSender.AI)
                .message(evaluation.nextMessage())
                .build());

        sessionRepository.save(session);
        return interviewMapper.toDto(session);
    }

    @Transactional
    public InterviewSessionDto complete(UUID userId, UUID sessionId) {
        InterviewSession session = sessionRepository.findWithMessagesByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Interview session", sessionId));
        if (session.getStatus() == InterviewStatus.COMPLETED) {
            return interviewMapper.toDto(session);
        }

        InterviewFeedback feedback = aiProviderResolver.current().generateFeedback(toConversation(session));
        session.setCommunicationScore(feedback.communicationScore());
        session.setTechnicalScore(feedback.technicalScore());
        session.setConfidenceScore(feedback.confidenceScore());
        session.setScore(feedback.overallScore());
        session.setFeedback(buildFeedbackText(feedback));
        session.setStatus(InterviewStatus.COMPLETED);

        sessionRepository.save(session);
        return interviewMapper.toDto(session);
    }

    @Transactional(readOnly = true)
    public InterviewSessionDto get(UUID userId, UUID sessionId) {
        return interviewMapper.toDto(sessionRepository.findWithMessagesByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Interview session", sessionId)));
    }

    @Transactional(readOnly = true)
    public PageResponse<InterviewSessionDto> history(UUID userId, Pageable pageable) {
        return PageResponse.from(sessionRepository.findByUserId(userId, pageable), interviewMapper::toDto);
    }

    // ------------------------------ helpers ----------------------------------

    private List<ChatTurn> toConversation(InterviewSession session) {
        return session.getMessages().stream()
                .map(m -> m.getSender() == MessageSender.AI
                        ? ChatTurn.assistant(m.getMessage())
                        : ChatTurn.user(m.getMessage()))
                .toList();
    }

    private String resolveResumeText(UUID userId, UUID resumeId) {
        return Optional.ofNullable(resumeId)
                .map(id -> resumeService.getOwnedResume(userId, id).getParsedText())
                .orElse("");
    }

    private String resolveJobText(UUID userId, UUID jobId) {
        return Optional.ofNullable(jobId)
                .map(id -> jobService.getOwnedJob(userId, id).getDescription())
                .orElse("");
    }

    private String buildFeedbackText(InterviewFeedback feedback) {
        return feedback.summary() + "\n\nImprovement areas:\n- "
                + String.join("\n- ", feedback.improvementAreas());
    }
}
