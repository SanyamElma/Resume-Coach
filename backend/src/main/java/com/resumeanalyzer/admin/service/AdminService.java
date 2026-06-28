package com.resumeanalyzer.admin.service;

import com.resumeanalyzer.admin.dto.AdminMetricsDto;
import com.resumeanalyzer.ai.observability.AiTelemetry;
import com.resumeanalyzer.ai.observability.AiUsageSnapshot;
import com.resumeanalyzer.analysis.repository.AnalysisReportRepository;
import com.resumeanalyzer.common.dto.PageResponse;
import com.resumeanalyzer.common.exception.BadRequestException;
import com.resumeanalyzer.common.exception.ResourceNotFoundException;
import com.resumeanalyzer.interview.repository.InterviewSessionRepository;
import com.resumeanalyzer.resume.repository.ResumeRepository;
import com.resumeanalyzer.user.domain.Role;
import com.resumeanalyzer.user.domain.User;
import com.resumeanalyzer.user.dto.UserDto;
import com.resumeanalyzer.user.mapper.UserMapper;
import com.resumeanalyzer.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Administrative operations: platform metrics and user management. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final InterviewSessionRepository sessionRepository;
    private final AnalysisReportRepository reportRepository;
    private final UserMapper userMapper;
    private final AiTelemetry aiTelemetry;

    /** Aggregate AI usage since startup (tokens, cost, cache-hit rate, latency, fallbacks). */
    public AiUsageSnapshot aiUsage() {
        return aiTelemetry.snapshot();
    }

    @Transactional(readOnly = true)
    public AdminMetricsDto metrics() {
        Instant now = Instant.now();
        return new AdminMetricsDto(
                userRepository.count(),
                resumeRepository.count(),
                sessionRepository.count(),
                reportRepository.count(),
                userRepository.countByCreatedAtAfter(now.minus(Duration.ofDays(7))),
                sessionRepository.countByCreatedAtAfter(now.minus(Duration.ofDays(1))));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> listUsers(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable), userMapper::toDto);
    }

    @Transactional
    public void deleteUser(UUID actingAdminId, UUID targetUserId) {
        if (actingAdminId.equals(targetUserId)) {
            throw new BadRequestException("Administrators cannot delete their own account");
        }
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", targetUserId));
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot delete another administrator account");
        }
        userRepository.delete(user); // cascades to resumes/jobs/sessions via FK ON DELETE CASCADE
        log.warn("Admin {} deleted user {}", actingAdminId, targetUserId);
    }
}
