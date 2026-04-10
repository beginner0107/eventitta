package com.eventitta.api.auth.jwt;

import com.eventitta.api.auth.jwt.JwtProperties;
import com.eventitta.domain.auth.port.AuthTokenProvider;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static com.eventitta.api.auth.AuthConstants.CLAIM_AUTH_VERSION;
import static com.eventitta.api.auth.AuthConstants.CLAIM_EMAIL;
import static com.eventitta.api.auth.AuthConstants.CLAIM_ROLE;
import static com.eventitta.api.auth.AuthConstants.CLAIM_SESSION_ID;
import static com.eventitta.domain.auth.exception.AuthErrorCode.ACCESS_TOKEN_EXPIRED;
import static com.eventitta.domain.auth.exception.AuthErrorCode.ACCESS_TOKEN_INVALID;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class JwtTokenProvider implements AuthTokenProvider {
    private final Key signingKey;
    @Getter
    private final long accessTokenValidityMs;
    @Getter
    private final long refreshTokenValidityMs;
    private final Clock clock;

    public JwtTokenProvider(JwtProperties props, Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(props.getSecret().getBytes(UTF_8));
        this.clock = clock;
        this.accessTokenValidityMs = props.getAccessTokenValidityMs();
        this.refreshTokenValidityMs = props.getRefreshTokenValidityMs();
    }

    private JwtParser parser() {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .setClock(() -> Date.from(clock.instant()))
            .build();
    }

    @Override
    public String createAccessToken(Long userId, String email, String role, long authVersion, String sessionId) {
        Instant now = clock.instant();
        return Jwts.builder()
            .setSubject(userId.toString())
            .claim(CLAIM_EMAIL, email)
            .claim(CLAIM_ROLE, role)
            .claim(CLAIM_AUTH_VERSION, authVersion)
            .claim(CLAIM_SESSION_ID, sessionId)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusMillis(accessTokenValidityMs)))
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }

    @Override
    public String createRefreshTokenKey() {
        byte[] randomBytes = KeyGenerators.secureRandom(16).generateKey();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @Override
    public String createRefreshTokenSecret() {
        byte[] randomBytes = KeyGenerators.secureRandom(32).generateKey();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @Override
    public Instant getRefreshTokenExpiry() {
        return clock.instant().plusMillis(refreshTokenValidityMs);
    }

    public ParsedAccessToken parseAccessToken(String token) {
        try {
            return toParsedAccessToken(parser().parseClaimsJws(token).getBody());
        } catch (ExpiredJwtException e) {
            throw ACCESS_TOKEN_EXPIRED.defaultException(e);
        } catch (JwtException | IllegalArgumentException e) {
            throw ACCESS_TOKEN_INVALID.defaultException(e);
        }
    }

    @Override
    public Long getUserId(String token) {
        return parseAccessToken(token).userId();
    }

    @Override
    public Long getUserIdFromExpiredToken(String token) {
        try {
            return toParsedAccessToken(parser().parseClaimsJws(token).getBody()).userId();
        } catch (ExpiredJwtException e) {
            return Long.parseLong(e.getClaims().getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw ACCESS_TOKEN_INVALID.defaultException(e);
        }
    }

    @Override
    public void validateAccessToken(String token) {
        parseAccessToken(token);
    }

    @Override
    public String getEmail(String token) {
        return parseAccessToken(token).email();
    }

    @Override
    public String getRole(String token) {
        return parseAccessToken(token).role();
    }

    private ParsedAccessToken toParsedAccessToken(Claims claims) {
        return new ParsedAccessToken(
            Long.parseLong(claims.getSubject()),
            claims.get(CLAIM_EMAIL, String.class),
            claims.get(CLAIM_ROLE, String.class),
            claims.get(CLAIM_AUTH_VERSION, Long.class) != null ? claims.get(CLAIM_AUTH_VERSION, Long.class) : 0L,
            claims.get(CLAIM_SESSION_ID, String.class),
            claims.getIssuedAt().toInstant(),
            claims.getExpiration().toInstant()
        );
    }
}
