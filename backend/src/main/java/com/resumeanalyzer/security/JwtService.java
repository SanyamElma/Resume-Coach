package com.resumeanalyzer.security;

import com.resumeanalyzer.config.properties.AppProperties;
import com.resumeanalyzer.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
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
        this.signingKey = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(props.secret()));
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
