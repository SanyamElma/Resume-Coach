package com.resumeanalyzer.analysis.controller;

import com.resumeanalyzer.analysis.dto.AnalysisReportDto;
import com.resumeanalyzer.analysis.dto.RunAnalysisRequest;
import com.resumeanalyzer.analysis.service.AnalysisService;
import com.resumeanalyzer.common.dto.ApiResponse;
import com.resumeanalyzer.common.dto.PageResponse;
import com.resumeanalyzer.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Tag(name = "Analysis", description = "Resume vs. job-description skill-gap analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/run")
    @Operation(summary = "Run a skill-gap analysis for a resume against a job description")
    public ResponseEntity<ApiResponse<AnalysisReportDto>> run(@Valid @RequestBody RunAnalysisRequest request) {
        AnalysisReportDto dto = analysisService.run(SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(dto, "Analysis complete"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single analysis report")
    public ResponseEntity<ApiResponse<AnalysisReportDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.get(SecurityUtils.currentUserId(), id)));
    }

    @GetMapping
    @Operation(summary = "List the current user's analysis reports (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<AnalysisReportDto>>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(analysisService.history(SecurityUtils.currentUserId(), pageable)));
    }
}
