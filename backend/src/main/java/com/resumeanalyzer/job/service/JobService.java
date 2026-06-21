package com.resumeanalyzer.job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.AiProviderResolver;
import com.resumeanalyzer.ai.model.JobAnalysis;
import com.resumeanalyzer.common.dto.PageResponse;
import com.resumeanalyzer.common.exception.ResourceNotFoundException;
import com.resumeanalyzer.job.domain.JobDescription;
import com.resumeanalyzer.job.dto.CreateJobRequest;
import com.resumeanalyzer.job.dto.JobDescriptionDto;
import com.resumeanalyzer.job.mapper.JobMapper;
import com.resumeanalyzer.job.repository.JobDescriptionRepository;
import com.resumeanalyzer.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for job descriptions. On creation the configured AI provider
 * structures the free text into required/preferred skills and keywords; structuring
 * failures degrade gracefully (the JD is still saved without structured data).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobDescriptionRepository jobRepository;
    private final AiProviderResolver aiProviderResolver;
    private final JobMapper jobMapper;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Transactional
    public JobDescriptionDto create(UUID userId, CreateJobRequest request) {
        JobDescription jd = JobDescription.builder()
                .user(userService.getUser(userId))
                .title(request.title().trim())
                .company(request.company())
                .description(request.description())
                .structuredData(structure(request.description()))
                .build();
        jobRepository.save(jd);
        return jobMapper.toDto(jd);
    }

    @Transactional(readOnly = true)
    public PageResponse<JobDescriptionDto> list(UUID userId, Pageable pageable) {
        return PageResponse.from(jobRepository.findByUserId(userId, pageable), jobMapper::toDto);
    }

    @Transactional(readOnly = true)
    public JobDescriptionDto get(UUID userId, UUID jobId) {
        return jobMapper.toDto(getOwnedJob(userId, jobId));
    }

    @Transactional(readOnly = true)
    public JobDescription getOwnedJob(UUID userId, UUID jobId) {
        return jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Job description", jobId));
    }

    private String structure(String description) {
        try {
            JobAnalysis analysis = aiProviderResolver.current().analyzeJobDescription(description);
            return objectMapper.writeValueAsString(analysis);
        } catch (Exception e) {
            log.warn("JD structuring failed; storing without structured data: {}", e.getMessage());
            return null;
        }
    }
}
