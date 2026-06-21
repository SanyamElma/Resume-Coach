package com.resumeanalyzer.job.controller;

import com.resumeanalyzer.common.dto.ApiResponse;
import com.resumeanalyzer.common.dto.PageResponse;
import com.resumeanalyzer.job.dto.CreateJobRequest;
import com.resumeanalyzer.job.dto.JobDescriptionDto;
import com.resumeanalyzer.job.service.JobService;
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
@RequestMapping("/api/job")
@RequiredArgsConstructor
@Tag(name = "Job Description", description = "Create and manage target job descriptions")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @Operation(summary = "Create a job description (AI-structured into skills/keywords)")
    public ResponseEntity<ApiResponse<JobDescriptionDto>> create(@Valid @RequestBody CreateJobRequest request) {
        JobDescriptionDto dto = jobService.create(SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(dto, "Job description saved"));
    }

    @GetMapping
    @Operation(summary = "List the current user's job descriptions (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<JobDescriptionDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(jobService.list(SecurityUtils.currentUserId(), pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single job description")
    public ResponseEntity<ApiResponse<JobDescriptionDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(jobService.get(SecurityUtils.currentUserId(), id)));
    }
}
