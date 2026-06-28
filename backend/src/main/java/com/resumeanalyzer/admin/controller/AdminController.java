package com.resumeanalyzer.admin.controller;

import com.resumeanalyzer.admin.dto.AdminMetricsDto;
import com.resumeanalyzer.admin.service.AdminService;
import com.resumeanalyzer.ai.observability.AiUsageSnapshot;
import com.resumeanalyzer.common.dto.ApiResponse;
import com.resumeanalyzer.common.dto.PageResponse;
import com.resumeanalyzer.security.SecurityUtils;
import com.resumeanalyzer.user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin-only endpoints. Access is enforced both by the URL rule in {@code SecurityConfig}
 * ({@code /api/admin/** → ROLE_ADMIN}) and method-level {@link PreAuthorize} as defence in depth.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Platform administration (ADMIN role required)")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/metrics")
    @Operation(summary = "Aggregate platform metrics")
    public ResponseEntity<ApiResponse<AdminMetricsDto>> metrics() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.metrics()));
    }

    @GetMapping("/ai-metrics")
    @Operation(summary = "Aggregate AI usage: tokens, cost, cache-hit rate, latency, fallbacks")
    public ResponseEntity<ApiResponse<AiUsageSnapshot>> aiMetrics() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.aiUsage()));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(adminService.listUsers(pageable)));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete a user and all of their data")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        adminService.deleteUser(SecurityUtils.currentUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok(null, "User deleted"));
    }
}
