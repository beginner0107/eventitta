package com.eventitta.auth.jwt;

import com.eventitta.auth.exception.AuthErrorCode;
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

import static com.eventitta.auth.exception.AuthErrorCode.ACCESS_TOKEN_EXPIRED;
import static com.eventitta.auth.exception.AuthErrorCode.ACCESS_TOKEN_INVALID;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class JwtTokenProvider {
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

    public String createAccessToken(Long userId) {
        Instant now = clock.instant();
        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusMillis(accessTokenValidityMs)))
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }

    public String createRefreshToken() {
        byte[] randomBytes = KeyGenerators.secureRandom(32).generateKey();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public Instant getRefreshTokenExpiry() {
        return clock.instant().plusMillis(refreshTokenValidityMs);
    }

    public boolean validateToken(String token) {
        try {
            parser().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        try {
            Claims claims = parser().parseClaimsJws(token).getBody();
            return Long.parseLong(claims.getSubject());
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            throw AuthErrorCode.ACCESS_TOKEN_INVALID.defaultException(e);
        }
    }

    public Long getUserIdFromExpiredToken(String token) {
        try {
            return getUserId(token);
        } catch (ExpiredJwtException e) {
            return Long.parseLong(e.getClaims().getSubject());
        }
    }

    public boolean validateAccessToken(String token) {
        try {
            parser().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw ACCESS_TOKEN_EXPIRED.defaultException(e);
        } catch (JwtException | IllegalArgumentException e) {
            throw ACCESS_TOKEN_INVALID.defaultException(e);
        }
    }
}
