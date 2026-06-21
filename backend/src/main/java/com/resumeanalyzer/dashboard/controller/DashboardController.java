package com.resumeanalyzer.dashboard.controller;

import com.resumeanalyzer.common.dto.ApiResponse;
import com.resumeanalyzer.dashboard.dto.DashboardDto;
import com.resumeanalyzer.dashboard.service.DashboardService;
import com.resumeanalyzer.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Candidate analytics dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Aggregated analytics for the current user's dashboard")
    public ResponseEntity<ApiResponse<DashboardDto>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.build(SecurityUtils.currentUserId())));
    }
}
