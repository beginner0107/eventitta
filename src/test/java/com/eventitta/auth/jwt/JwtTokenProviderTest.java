package com.eventitta.auth.jwt;

import com.eventitta.auth.exception.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static com.eventitta.auth.exception.AuthErrorCode.ACCESS_TOKEN_EXPIRED;
import static com.eventitta.auth.exception.AuthErrorCode.ACCESS_TOKEN_INVALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("토큰 프로바이더 단위 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider provider;
    private final String secret = "0123456789abcdef0123456789abcdef";
    private final long accessValidity = 500;
    private final long refreshValidity = 1000;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(secret);
        props.setAccessTokenValidityMs(accessValidity);
        props.setRefreshTokenValidityMs(refreshValidity);
        provider = new JwtTokenProvider(props);
    }

    @Test
    @DisplayName("엑세스 토큰이 유효하면 토큰에서 사용자 ID를 추출한다.")
    void givenUserId_whenCreateAccessToken_thenIsParsable() {
        // given
        String token = provider.createAccessToken(123L);

        // when & then
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo(123L);
    }

    @Test
    @DisplayName("만료된 토큰을 검증하면 false를 반환해야 한다.")
    void givenExpiredToken_whenValidateToken_thenReturnsFalse() throws InterruptedException {
        // given
        String token = provider.createAccessToken(99L);
        Thread.sleep(accessValidity + 100);

        // when & then
        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰에서 사용자 ID를 추출하면 올바른 ID를 반환해야 한다.")
    void givenExpiredToken_whenGetUserIdFromExpiredToken_thenReturnsId() throws InterruptedException {
        // given
        String token = provider.createAccessToken(77L);
        Thread.sleep(accessValidity + 100);

        // when & then
        Long userId = provider.getUserIdFromExpiredToken(token);
        assertThat(userId).isEqualTo(77L);
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
    @DisplayName("만료된 토큰으로 호출 시 토큰 만료 예외가 발생해야 한다.")
    void givenExpiredToken_whenValidateAccessToken_thenThrowsExpired() throws InterruptedException {
        // given
        String token = provider.createAccessToken(55L);
        Thread.sleep(accessValidity + 100);

        // when & then
        assertThatThrownBy(() -> provider.validateAccessToken(token))
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
        Instant now = Instant.now();
        Instant expiry = provider.getRefreshTokenExpiry();

        // when & then
        assertThat(expiry).isAfterOrEqualTo(now.plusMillis(refreshValidity - 10));
    }
}
