package com.resumeanalyzer.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.analysis.domain.AnalysisReport;
import com.resumeanalyzer.analysis.repository.AnalysisReportRepository;
import com.resumeanalyzer.common.util.JsonLists;
import com.resumeanalyzer.dashboard.dto.DashboardDto;
import com.resumeanalyzer.interview.domain.InterviewSession;
import com.resumeanalyzer.interview.domain.InterviewStatus;
import com.resumeanalyzer.interview.repository.InterviewSessionRepository;
import com.resumeanalyzer.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the candidate analytics dashboard by aggregating analysis reports and interview
 * sessions into trend and frequency series.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AnalysisReportRepository reportRepository;
    private final InterviewSessionRepository sessionRepository;
    private final ResumeRepository resumeRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public DashboardDto build(UUID userId) {
        List<AnalysisReport> reports = reportRepository.findAllForUserOrderByCreatedAt(userId);
        List<InterviewSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtAsc(userId);

        List<DashboardDto.TrendPoint> resumeTrend = reports.stream()
                .map(r -> new DashboardDto.TrendPoint(r.getCreatedAt(), r.getMatchPercentage(),
                        r.getJobDescription().getTitle()))
                .toList();

        List<DashboardDto.TrendPoint> interviewTrend = sessions.stream()
                .filter(s -> s.getStatus() == InterviewStatus.COMPLETED && s.getScore() != null)
                .map(s -> new DashboardDto.TrendPoint(s.getCreatedAt(), s.getScore(), "Interview"))
                .toList();

        Integer latestMatch = reports.isEmpty() ? null
                : reports.get(reports.size() - 1).getMatchPercentage();

        return new DashboardDto(
                resumeRepository.countByUserId(userId),
                reports.size(),
                sessions.size(),
                latestMatch,
                resumeTrend,
                interviewTrend,
                topStrengths(reports),
                missingSkillFrequency(reports));
    }

    /** Frequency of strength signals across reports → skill distribution chart. */
    private List<DashboardDto.FrequencyPoint> topStrengths(List<AnalysisReport> reports) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (AnalysisReport report : reports) {
            for (String strength : JsonLists.fromJson(objectMapper, report.getStrengths())) {
                counts.merge(normalize(strength), 1, Integer::sum);
            }
        }
        return toSortedPoints(counts, 8);
    }

    /** Frequency of missing skills across reports → missing-skills chart. */
    private List<DashboardDto.FrequencyPoint> missingSkillFrequency(List<AnalysisReport> reports) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (AnalysisReport report : reports) {
            for (String skill : JsonLists.fromJson(objectMapper, report.getMissingSkills())) {
                counts.merge(normalize(skill), 1, Integer::sum);
            }
        }
        return toSortedPoints(counts, 8);
    }

    private List<DashboardDto.FrequencyPoint> toSortedPoints(Map<String, Integer> counts, int limit) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new DashboardDto.FrequencyPoint(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(DashboardDto.FrequencyPoint::count).reversed())
                .toList();
    }

    private String normalize(String s) {
        String trimmed = s == null ? "" : s.replaceAll("^(Strong match on|Limited evidence of)\\s*", "").trim();
        return trimmed.length() > 40 ? trimmed.substring(0, 40) : trimmed;
    }
}
