package com.eventitta.auth.jwt;

import com.eventitta.auth.exception.AuthException;
import com.eventitta.auth.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.eventitta.auth.exception.AuthErrorCode.ACCESS_TOKEN_EXPIRED;
import static com.eventitta.auth.exception.AuthErrorCode.ACCESS_TOKEN_INVALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("토큰 프로바이더 단위 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider provider;
    private Clock baseClock;
    private JwtProperties props;

    private final String secret = "0123456789abcdef0123456789abcdef";
    private final long accessValidity = 60_000L;
    private final long refreshValidity = 1000L;


    @BeforeEach
    void setUp() {
        baseClock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

        props = new JwtProperties();
        props.setSecret(secret);
        props.setAccessTokenValidityMs(accessValidity);
        props.setRefreshTokenValidityMs(refreshValidity);
        provider = new JwtTokenProvider(props, baseClock);
    }

    @Test
    @DisplayName("엑세스 토큰이 유효하면 토큰에서 사용자 정보를 추출한다.")
    void givenUserId_whenCreateAccessToken_thenIsParsable() {
        // given
        String token = provider.createAccessToken(123L, "test@example.com", "USER");

        // when & then
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo(123L);
        assertThat(provider.getEmail(token)).isEqualTo("test@example.com");
        assertThat(provider.getRole(token)).isEqualTo("USER");
    }

    @Test
    @DisplayName("만료된 토큰을 검증하면 false를 반환해야 한다.")
    void givenExpiredToken_whenValidateToken_thenReturnsFalse() throws InterruptedException {
        // given
        String token = provider.createAccessToken(99L, "test@example.com", "USER");

        Clock laterClock = Clock.offset(baseClock, Duration.ofMillis(accessValidity + 1));
        JwtTokenProvider expiredProvider = new JwtTokenProvider(props, laterClock);

        // when & then
        assertThat(expiredProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰에서 사용자 ID를 추출하면 올바른 ID를 반환해야 한다.")
    void givenExpiredToken_whenGetUserIdFromExpiredToken_thenReturnsId() {
        // given
        String token = provider.createAccessToken(77L, "test@example.com", "USER");

        Clock laterClock = Clock.offset(baseClock, Duration.ofMillis(accessValidity + 1));
        JwtTokenProvider expiredProvider = new JwtTokenProvider(props, laterClock);

        // when & then
        assertThat(expiredProvider.getUserIdFromExpiredToken(token)).isEqualTo(77L);
    }

    @Test
    @DisplayName("잘못된 형식의 토큰으로 엑세스 토큰 검증 메서드를 호출 시 예외가 발생해야 한다.")
    void givenMalformedToken_whenValidateAccessToken_thenThrowsInvalid() {
        // when & then
        assertThatThrownBy(() -> provider.validateAccessToken("not.a.jwt.token"))
            .isInstanceOf(AuthException.class)
            .extracting("errorCode")
            .isEqualTo(ACCESS_TOKEN_INVALID);
    }

    @Test
    @DisplayName("만료된 토큰으로 호출 시 토큰 만료 예외가 발생해야 한다")
    void givenExpiredToken_whenValidateAccessToken_thenThrowsExpired() {
        // given
        String token = provider.createAccessToken(55L, "test@example.com", "USER");
        Clock laterClock = Clock.offset(baseClock, Duration.ofMillis(accessValidity + 1));
        JwtTokenProvider expiredProvider = new JwtTokenProvider(props, laterClock);

        // when & then
        assertThatThrownBy(() -> expiredProvider.validateAccessToken(token))
            .isInstanceOf(AuthException.class)
            .extracting("errorCode")
            .isEqualTo(ACCESS_TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("리프레시 토큰 생성은 빈 문자열이 아니며 매번 다른 값을 반환해야 한다.")
    void createRefreshToken_returnsRandomString() {
        // given
        String rt1 = provider.createRefreshToken();
        String rt2 = provider.createRefreshToken();

        // when & then
        assertThat(rt1).isNotEmpty();
        assertThat(rt2).isNotEmpty();
        assertThat(rt1).isNotEqualTo(rt2);
    }

    @Test
    @DisplayName("리프레시 토큰 만료 시각은 현재 시각 이후여야 한다.")
    void getRefreshTokenExpiry_returnsFutureInstant() {
        // given
        Instant expected = baseClock.instant().plusMillis(refreshValidity);

        // when
        Instant actual = provider.getRefreshTokenExpiry();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("유효한 토큰에서 이메일을 추출한다.")
    void givenValidToken_whenGetEmail_thenReturnsEmail() {
        // given
        String token = provider.createAccessToken(123L, "user@example.com", "ADMIN");

        // when & then
        assertThat(provider.getEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("유효한 토큰에서 역할을 추출한다.")
    void givenValidToken_whenGetRole_thenReturnsRole() {
        // given
        String token = provider.createAccessToken(123L, "user@example.com", "ADMIN");

        // when & then
        assertThat(provider.getRole(token)).isEqualTo("ADMIN");
    }
}
