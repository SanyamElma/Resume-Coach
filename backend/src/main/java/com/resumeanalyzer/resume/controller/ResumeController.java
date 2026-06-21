package com.resumeanalyzer.resume.controller;

import com.resumeanalyzer.common.dto.ApiResponse;
import com.resumeanalyzer.common.dto.PageResponse;
import com.resumeanalyzer.resume.dto.ResumeDto;
import com.resumeanalyzer.resume.dto.ResumeSummaryDto;
import com.resumeanalyzer.resume.service.ResumeService;
import com.resumeanalyzer.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
@Tag(name = "Resume", description = "Upload, parse, and manage resumes")
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a PDF resume; extracts text and structured fields")
    public ResponseEntity<ApiResponse<ResumeDto>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name) {
        ResumeDto dto = resumeService.upload(SecurityUtils.currentUserId(), file, name);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(dto, "Resume uploaded and parsed"));
    }

    @GetMapping
    @Operation(summary = "List the current user's resumes (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<ResumeSummaryDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(
                resumeService.list(SecurityUtils.currentUserId(), pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single resume with parsed content")
    public ResponseEntity<ApiResponse<ResumeDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(resumeService.get(SecurityUtils.currentUserId(), id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a resume and its stored file")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        resumeService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Resume deleted"));
    }
}
