package com.resumeanalyzer.user.controller;

import com.resumeanalyzer.common.dto.ApiResponse;
import com.resumeanalyzer.security.SecurityUtils;
import com.resumeanalyzer.user.dto.UpdateProfileRequest;
import com.resumeanalyzer.user.dto.UserDto;
import com.resumeanalyzer.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Authenticated user's own profile")
public class ProfileController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get the current user's profile")
    public ResponseEntity<ApiResponse<UserDto>> getProfile() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(SecurityUtils.currentUserId())));
    }

    @PutMapping
    @Operation(summary = "Update the current user's profile")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.updateProfile(SecurityUtils.currentUserId(), request), "Profile updated"));
    }
}
