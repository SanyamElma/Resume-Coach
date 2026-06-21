package com.resumeanalyzer.auth.dto;

import com.resumeanalyzer.user.dto.UserDto;

/** Token bundle returned on successful authentication. */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserDto user
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserDto user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
