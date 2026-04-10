package com.eventitta.api.auth.jwt;

import com.eventitta.api.auth.properties.JwtProperties;
import com.eventitta.domain.auth.exception.AuthErrorCode;
import com.eventitta.domain.auth.exception.AuthException;
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
        String expiredAccessToken = issuer.createAccessToken(1L, "spring@test.com", "USER", 3L, "session-1");

        JwtTokenProvider parser = new JwtTokenProvider(jwtProperties(), Clock.fixed(issuedAt.plusSeconds(2), ZoneOffset.UTC));

        assertThat(parser.getUserIdFromExpiredToken(expiredAccessToken)).isEqualTo(1L);
    }

    @Test
    @DisplayName("refresh token key 와 secret 는 서로 다른 난수 문자열로 생성된다")
    void createRefreshTokenParts_returnsOpaqueValues() {
        JwtTokenProvider provider = new JwtTokenProvider(jwtProperties(), Clock.fixed(Instant.now(), ZoneOffset.UTC));

        String key = provider.createRefreshTokenKey();
        String secret = provider.createRefreshTokenSecret();

        assertThat(key).isNotBlank();
        assertThat(secret).isNotBlank();
        assertThat(secret).isNotEqualTo(key);
    }

    @Test
    @DisplayName("access token 을 한 번 파싱하면 principal 생성에 필요한 claim 을 함께 얻는다")
    void parseAccessToken_returnsAllClaims() {
        Instant issuedAt = Instant.parse("2026-03-15T00:00:00Z");
        JwtTokenProvider provider = new JwtTokenProvider(jwtProperties(), Clock.fixed(issuedAt, ZoneOffset.UTC));
        String accessToken = provider.createAccessToken(7L, "spring@test.com", "USER", 5L, "session-7");

        ParsedAccessToken parsedAccessToken = provider.parseAccessToken(accessToken);

        assertThat(parsedAccessToken.userId()).isEqualTo(7L);
        assertThat(parsedAccessToken.email()).isEqualTo("spring@test.com");
        assertThat(parsedAccessToken.role()).isEqualTo("USER");
        assertThat(parsedAccessToken.authVersion()).isEqualTo(5L);
        assertThat(parsedAccessToken.sessionId()).isEqualTo("session-7");
        assertThat(parsedAccessToken.issuedAt()).isEqualTo(issuedAt);
        assertThat(parsedAccessToken.expiresAt()).isEqualTo(issuedAt.plusSeconds(1));
    }

    @Test
    @DisplayName("서명이 변조된 access token 은 ACCESS_TOKEN_INVALID 예외를 던진다")
    void getUserIdFromExpiredToken_throwsAuthExceptionForTamperedToken() {
        JwtTokenProvider provider = new JwtTokenProvider(jwtProperties(), Clock.fixed(Instant.parse("2026-03-15T00:00:00Z"), ZoneOffset.UTC));
        String accessToken = provider.createAccessToken(1L, "spring@test.com", "USER", 0L, "session-1");
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
