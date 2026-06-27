package com.resumeanalyzer.security;

import com.resumeanalyzer.config.properties.AppProperties;
import com.resumeanalyzer.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates signed JWT access tokens.
 *
 * <p>Access tokens are short-lived and stateless: the subject is the user id and a custom
 * {@code role} claim drives authorization. Refresh tokens are handled separately and
 * persisted (see {@code RefreshTokenService}) so they can be revoked.</p>
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final AppProperties.Security.Jwt props;

    public JwtService(AppProperties properties) {
        this.props = properties.security().jwt();
        // Derive a fixed 256-bit HMAC key from the configured secret via SHA-256. This
        // accepts ANY non-empty secret string (raw or Base64) — no length/encoding
        // constraints — so platform-generated secrets (e.g. Render) work out of the box.
        this.signingKey = Keys.hmacShaKeyFor(sha256(props.secret()));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    /** Generates a signed access token for the given user. */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(props.issuer())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.accessTokenTtl())))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
