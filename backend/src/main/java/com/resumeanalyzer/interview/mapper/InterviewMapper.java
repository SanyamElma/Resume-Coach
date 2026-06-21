package com.resumeanalyzer.interview.mapper;

import com.resumeanalyzer.interview.domain.InterviewMessage;
import com.resumeanalyzer.interview.domain.InterviewSession;
import com.resumeanalyzer.interview.dto.InterviewMessageDto;
import com.resumeanalyzer.interview.dto.InterviewSessionDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InterviewMapper {

    public InterviewMessageDto toDto(InterviewMessage message) {
        return new InterviewMessageDto(message.getId(), message.getSender(), message.getMessage(),
                message.getAnswerScore(), message.getCreatedAt());
    }

    public InterviewSessionDto toDto(InterviewSession session) {
        List<InterviewMessageDto> messages = session.getMessages().stream().map(this::toDto).toList();
        return new InterviewSessionDto(session.getId(), session.getStatus(), session.getScore(),
                session.getCommunicationScore(), session.getTechnicalScore(), session.getConfidenceScore(),
                session.getFeedback(), session.getCreatedAt(), messages);
    }
}
