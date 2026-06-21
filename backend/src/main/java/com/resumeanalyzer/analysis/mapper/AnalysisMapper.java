package com.resumeanalyzer.analysis.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.analysis.domain.AnalysisReport;
import com.resumeanalyzer.analysis.dto.AnalysisReportDto;
import com.resumeanalyzer.common.util.JsonLists;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalysisMapper {

    private final ObjectMapper objectMapper;

    public AnalysisReportDto toDto(AnalysisReport report) {
        return new AnalysisReportDto(
                report.getId(),
                report.getResume().getId(),
                report.getResume().getResumeName(),
                report.getJobDescription().getId(),
                report.getJobDescription().getTitle(),
                report.getMatchPercentage(),
                report.getSkillMatchScore(),
                report.getExperienceMatchScore(),
                report.getEducationMatchScore(),
                report.getKeywordMatchScore(),
                JsonLists.fromJson(objectMapper, report.getMissingSkills()),
                JsonLists.fromJson(objectMapper, report.getStrengths()),
                JsonLists.fromJson(objectMapper, report.getWeaknesses()),
                JsonLists.fromJson(objectMapper, report.getRecommendations()),
                report.getCreatedAt());
    }
}
