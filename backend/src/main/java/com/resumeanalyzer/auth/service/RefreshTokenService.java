package com.resumeanalyzer.auth.service;

import com.resumeanalyzer.auth.domain.RefreshToken;
import com.resumeanalyzer.auth.repository.RefreshTokenRepository;
import com.resumeanalyzer.common.exception.BadRequestException;
import com.resumeanalyzer.config.properties.AppProperties;
import com.resumeanalyzer.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Manages the lifecycle of persisted refresh tokens, including issuance, validation, and
 * rotation. The raw token is returned to the caller once; only its hash is stored.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppProperties appProperties;

    /** Issues a new refresh token for the user and returns the raw (un-hashed) value. */
    @Transactional
    public String issue(User user) {
        String raw = TokenHasher.generateRawToken();
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256(raw))
                .expiresAt(Instant.now().plus(appProperties.security().jwt().refreshTokenTtl()))
                .build();
        refreshTokenRepository.save(token);
        return raw;
    }

    /** Validates an incoming raw refresh token and returns its owning user. */
    @Transactional(readOnly = true)
    public User verify(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (!token.isActive()) {
            throw new BadRequestException("Refresh token expired or revoked");
        }
        return token.getUser();
    }

    /** Rotates a refresh token: revokes the presented one and issues a fresh token. */
    @Transactional
    public String rotate(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (!token.isActive()) {
            throw new BadRequestException("Refresh token expired or revoked");
        }
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        return issue(token.getUser());
    }

    /** Revokes every active refresh token for the user (logout-everywhere semantics). */
    @Transactional
    public void revokeAll(User user) {
        refreshTokenRepository.revokeAllForUser(user);
    }
}
