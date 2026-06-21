package com.resumeanalyzer.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.AiProviderResolver;
import com.resumeanalyzer.ai.model.SkillGapResult;
import com.resumeanalyzer.analysis.domain.AnalysisReport;
import com.resumeanalyzer.analysis.dto.AnalysisReportDto;
import com.resumeanalyzer.analysis.dto.RunAnalysisRequest;
import com.resumeanalyzer.analysis.mapper.AnalysisMapper;
import com.resumeanalyzer.analysis.repository.AnalysisReportRepository;
import com.resumeanalyzer.common.dto.PageResponse;
import com.resumeanalyzer.common.exception.ResourceNotFoundException;
import com.resumeanalyzer.common.util.JsonLists;
import com.resumeanalyzer.job.domain.JobDescription;
import com.resumeanalyzer.job.service.JobService;
import com.resumeanalyzer.resume.domain.Resume;
import com.resumeanalyzer.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Runs the skill-gap analysis: pulls the owner's resume and job description, delegates the
 * comparison to the active AI provider, and persists the resulting report.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisReportRepository reportRepository;
    private final AiProviderResolver aiProviderResolver;
    private final ResumeService resumeService;
    private final JobService jobService;
    private final AnalysisMapper analysisMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public AnalysisReportDto run(UUID userId, RunAnalysisRequest request) {
        Resume resume = resumeService.getOwnedResume(userId, request.resumeId());
        JobDescription jd = jobService.getOwnedJob(userId, request.jobDescriptionId());

        SkillGapResult result = aiProviderResolver.current()
                .analyzeSkillGap(resume.getParsedText(), jd.getDescription());

        AnalysisReport report = AnalysisReport.builder()
                .resume(resume)
                .jobDescription(jd)
                .matchPercentage(result.matchScore())
                .skillMatchScore(result.skillMatchScore())
                .experienceMatchScore(result.experienceMatchScore())
                .educationMatchScore(result.educationMatchScore())
                .keywordMatchScore(result.keywordMatchScore())
                .missingSkills(JsonLists.toJson(objectMapper, result.missingSkills()))
                .strengths(JsonLists.toJson(objectMapper, result.strengths()))
                .weaknesses(JsonLists.toJson(objectMapper, result.weaknesses()))
                .recommendations(JsonLists.toJson(objectMapper, result.recommendations()))
                .build();

        reportRepository.save(report);
        log.info("Analysis {} created for resume {} vs job {}", report.getId(), resume.getId(), jd.getId());
        return analysisMapper.toDto(report);
    }

    @Transactional(readOnly = true)
    public AnalysisReportDto get(UUID userId, UUID reportId) {
        AnalysisReport report = reportRepository.findByIdForUser(reportId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Analysis report", reportId));
        return analysisMapper.toDto(report);
    }

    @Transactional(readOnly = true)
    public PageResponse<AnalysisReportDto> history(UUID userId, Pageable pageable) {
        return PageResponse.from(reportRepository.findAllForUser(userId, pageable), analysisMapper::toDto);
    }
}
