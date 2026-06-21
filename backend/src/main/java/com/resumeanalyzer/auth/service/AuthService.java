package com.resumeanalyzer.auth.service;

import com.resumeanalyzer.auth.domain.PasswordResetToken;
import com.resumeanalyzer.auth.dto.AuthResponse;
import com.resumeanalyzer.auth.dto.ForgotPasswordRequest;
import com.resumeanalyzer.auth.dto.LoginRequest;
import com.resumeanalyzer.auth.dto.RegisterRequest;
import com.resumeanalyzer.auth.dto.ResetPasswordRequest;
import com.resumeanalyzer.auth.repository.PasswordResetTokenRepository;
import com.resumeanalyzer.common.exception.BadRequestException;
import com.resumeanalyzer.common.exception.ConflictException;
import com.resumeanalyzer.config.properties.AppProperties;
import com.resumeanalyzer.security.JwtService;
import com.resumeanalyzer.user.domain.Role;
import com.resumeanalyzer.user.domain.User;
import com.resumeanalyzer.user.mapper.UserMapper;
import com.resumeanalyzer.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Orchestrates the authentication use-cases: registration, login, token refresh, logout,
 * and the forgot/reset password flow. Issues a JWT access token plus a rotating refresh
 * token on each successful credential exchange.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final AppProperties appProperties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = User.builder()
                .name(request.name().trim())
                .email(request.email().toLowerCase().trim())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CANDIDATE)
                .enabled(true)
                .build();
        userRepository.save(user);
        log.info("Registered new candidate {}", user.getEmail());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Delegates credential verification (including BCrypt match) to Spring Security.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        User user = refreshTokenService.verify(rawRefreshToken);
        String newRefresh = refreshTokenService.rotate(rawRefreshToken);
        return buildResponse(user, newRefresh);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        User user = refreshTokenService.verify(rawRefreshToken);
        refreshTokenService.revokeAll(user);
        log.info("User {} logged out; refresh tokens revoked", user.getEmail());
    }

    /**
     * Begins the password reset flow. Always returns silently to avoid leaking which
     * emails are registered. In production the raw token is emailed; here it is logged.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            String raw = TokenHasher.generateRawToken();
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(TokenHasher.sha256(raw))
                    .expiresAt(Instant.now().plus(RESET_TOKEN_TTL))
                    .build();
            resetTokenRepository.save(token);
            // TODO: replace with EmailService.send(...) — see DEPLOYMENT.md
            log.info("Password reset requested for {}. Reset token (dev only): {}", user.getEmail(), raw);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokenRepository.findByTokenHash(TokenHasher.sha256(request.token()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));
        if (!token.isValid()) {
            throw new BadRequestException("Invalid or expired reset token");
        }
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        token.setUsed(true);
        refreshTokenService.revokeAll(user); // force re-login everywhere after a reset
        log.info("Password reset completed for {}", user.getEmail());
    }

    private AuthResponse issueTokens(User user) {
        String refresh = refreshTokenService.issue(user);
        return buildResponse(user, refresh);
    }

    private AuthResponse buildResponse(User user, String refreshToken) {
        String accessToken = jwtService.generateAccessToken(user);
        long expiresIn = appProperties.security().jwt().accessTokenTtl().toSeconds();
        return AuthResponse.of(accessToken, refreshToken, expiresIn, userMapper.toDto(user));
    }
}
