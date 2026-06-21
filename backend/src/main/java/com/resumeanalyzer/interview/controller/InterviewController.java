package com.resumeanalyzer.interview.controller;

import com.resumeanalyzer.ai.model.InterviewQuestion;
import com.resumeanalyzer.common.dto.ApiResponse;
import com.resumeanalyzer.common.dto.PageResponse;
import com.resumeanalyzer.interview.dto.GenerateQuestionsRequest;
import com.resumeanalyzer.interview.dto.InterviewSessionDto;
import com.resumeanalyzer.interview.dto.SendMessageRequest;
import com.resumeanalyzer.interview.dto.StartInterviewRequest;
import com.resumeanalyzer.interview.service.InterviewService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
@Tag(name = "Interview", description = "Interview question generation and AI mock interviews")
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/questions")
    @Operation(summary = "Generate tailored interview questions (default 20)")
    public ResponseEntity<ApiResponse<List<InterviewQuestion>>> generate(
            @Valid @RequestBody GenerateQuestionsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                interviewService.generateQuestions(SecurityUtils.currentUserId(), request)));
    }

    @PostMapping("/start")
    @Operation(summary = "Start a new mock interview session")
    public ResponseEntity<ApiResponse<InterviewSessionDto>> start(
            @Valid @RequestBody StartInterviewRequest request) {
        InterviewSessionDto dto = interviewService.start(SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(dto, "Interview started"));
    }

    @PostMapping("/message")
    @Operation(summary = "Send an answer and receive the interviewer's next turn")
    public ResponseEntity<ApiResponse<InterviewSessionDto>> message(
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                interviewService.sendMessage(SecurityUtils.currentUserId(), request)));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a session and generate final scored feedback")
    public ResponseEntity<ApiResponse<InterviewSessionDto>> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                interviewService.complete(SecurityUtils.currentUserId(), id), "Interview completed"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single interview session with its full transcript")
    public ResponseEntity<ApiResponse<InterviewSessionDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(interviewService.get(SecurityUtils.currentUserId(), id)));
    }

    @GetMapping("/history")
    @Operation(summary = "List the current user's interview sessions (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<InterviewSessionDto>>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(
                interviewService.history(SecurityUtils.currentUserId(), pageable)));
    }
}
