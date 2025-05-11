package com.eventitta.auth.jwt;

import com.eventitta.auth.exception.AuthErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static com.eventitta.auth.exception.AuthErrorCode.*;

@Component
public class JwtTokenProvider {
    private final Key signingKey;
    @Getter
    private final long accessTokenValidityMs;
    @Getter
    private final long refreshTokenValidityMs;

    public JwtTokenProvider(JwtProperties props) {
        this.signingKey = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = props.getAccessTokenValidityMs();
        this.refreshTokenValidityMs = props.getRefreshTokenValidityMs();
    }

    public String createAccessToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + accessTokenValidityMs))
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }

    public String createRefreshToken() {
        byte[] randomBytes = KeyGenerators.secureRandom(32).generateKey();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public Instant getRefreshTokenExpiry() {
        return Instant.now().plusMillis(refreshTokenValidityMs);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
        return Long.parseLong(claims.getSubject());
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
            Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw ACCESS_TOKEN_EXPIRED.defaultException(e);
        } catch (JwtException | IllegalArgumentException e) {
            throw ACCESS_TOKEN_INVALID.defaultException(e);
        }
    }
}
