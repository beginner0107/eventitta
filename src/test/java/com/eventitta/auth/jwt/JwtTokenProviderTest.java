package com.eventitta.auth.jwt;

import com.eventitta.auth.exception.AuthErrorCode;
import com.eventitta.auth.exception.AuthException;
import com.eventitta.auth.properties.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    @Test
    @DisplayName("만료된 access token 에서도 사용자 ID를 추출한다")
    void getUserIdFromExpiredToken_returnsUserIdForExpiredToken() {
        Instant issuedAt = Instant.parse("2026-03-15T00:00:00Z");
        JwtTokenProvider issuer = new JwtTokenProvider(jwtProperties(), Clock.fixed(issuedAt, ZoneOffset.UTC));
        String expiredAccessToken = issuer.createAccessToken(1L, "spring@test.com", "USER");

        JwtTokenProvider parser = new JwtTokenProvider(
            jwtProperties(),
            Clock.fixed(issuedAt.plusSeconds(2), ZoneOffset.UTC)
        );

        Long userId = parser.getUserIdFromExpiredToken(expiredAccessToken);

        assertThat(userId).isEqualTo(1L);
    }

    @Test
    @DisplayName("서명이 변조된 access token 은 ACCESS_TOKEN_INVALID 예외를 던진다")
    void getUserIdFromExpiredToken_throwsAuthExceptionForTamperedToken() {
        JwtTokenProvider provider = new JwtTokenProvider(
            jwtProperties(),
            Clock.fixed(Instant.parse("2026-03-15T00:00:00Z"), ZoneOffset.UTC)
        );
        String accessToken = provider.createAccessToken(1L, "spring@test.com", "USER");
        String tamperedAccessToken = tamperSignature(accessToken);

        assertThatThrownBy(() -> provider.getUserIdFromExpiredToken(tamperedAccessToken))
            .isInstanceOf(AuthException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.ACCESS_TOKEN_INVALID);
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789abcdef0123456789abcdef0123456789abcdef");
        properties.setAccessTokenValidityMs(1_000L);
        properties.setRefreshTokenValidityMs(86_400_000L);
        return properties;
    }

    private String tamperSignature(String token) {
        String[] parts = token.split("\\.");
        String signature = parts[2];
        char replacement = signature.charAt(signature.length() - 1) == 'a' ? 'b' : 'a';
        parts[2] = signature.substring(0, signature.length() - 1) + replacement;
        return String.join(".", parts);
    }
}
