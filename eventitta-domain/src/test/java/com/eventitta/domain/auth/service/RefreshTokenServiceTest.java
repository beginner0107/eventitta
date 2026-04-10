package com.eventitta.domain.auth.service;

import com.eventitta.domain.auth.domain.RefreshToken;
import com.eventitta.domain.auth.port.AuthTokenProvider;
import com.eventitta.domain.auth.repository.RefreshTokenRepository;
import com.eventitta.domain.auth.service.dto.ClientSessionMetadata;
import com.eventitta.domain.auth.service.dto.RefreshCommand;
import com.eventitta.domain.auth.service.dto.TokenResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.eventitta.domain.auth.exception.AuthErrorCode.ACCESS_TOKEN_INVALID;
import static com.eventitta.domain.auth.exception.AuthErrorCode.REFRESH_TOKEN_EXPIRED;
import static com.eventitta.domain.auth.exception.AuthErrorCode.REFRESH_TOKEN_INVALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private AuthTokenProvider authTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private Pbkdf2PasswordEncoder refreshTokenEncoder;

    @Mock
    private TokenService tokenService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-27T10:00:00Z"), ZoneOffset.UTC);

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(
            authTokenProvider,
            refreshTokenRepository,
            refreshTokenEncoder,
            tokenService,
            clock
        );
    }

    @Test
    @DisplayName("유효한 refresh token 은 row lock 후 같은 session 을 회전시킨다")
    void refresh_success() {
        ClientSessionMetadata sessionMetadata = new ClientSessionMetadata(
            "10.0.0.0/24",
            "Mozilla/5.0",
            Instant.parse("2026-03-27T10:05:00Z")
        );
        RefreshToken refreshToken = RefreshToken.issue(
            1L,
            "session-id-1",
            "refresh-key",
            "hashed-secret",
            LocalDateTime.now(clock).plusHours(1),
            LocalDateTime.now(clock),
            "192.168.0.0/24",
            "OldAgent/1.0"
        );
        given(refreshTokenRepository.findByTokenKeyForUpdate("refresh-key")).willReturn(Optional.of(refreshToken));
        given(refreshTokenEncoder.matches("refresh-secret", "hashed-secret")).willReturn(true);
        given(tokenService.rotateTokens(refreshToken, sessionMetadata)).willReturn(new TokenResult("new-access", "new-refresh"));

        TokenResult result = refreshTokenService.refresh(new RefreshCommand(null, "refresh-key.refresh-secret", sessionMetadata));

        assertThat(result.accessToken()).isEqualTo("new-access");
        then(tokenService).should().rotateTokens(refreshToken, sessionMetadata);
    }

    @Test
    @DisplayName("refresh token secret mismatch 는 row 삭제 후 REFRESH_TOKEN_INVALID 를 던진다")
    void refresh_invalidSecret() {
        RefreshToken refreshToken = RefreshToken.issue(
            1L,
            "session-id-1",
            "refresh-key",
            "hashed-secret",
            LocalDateTime.now(clock).plusHours(1),
            LocalDateTime.now(clock),
            null,
            null
        );
        given(refreshTokenRepository.findByTokenKeyForUpdate("refresh-key")).willReturn(Optional.of(refreshToken));
        given(refreshTokenEncoder.matches("wrong-secret", "hashed-secret")).willReturn(false);

        assertThatThrownBy(() -> refreshTokenService.refresh(new RefreshCommand(null, "refresh-key.wrong-secret")))
            .extracting("errorCode")
            .isEqualTo(REFRESH_TOKEN_INVALID);

        then(refreshTokenRepository).should().delete(refreshToken);
    }

    @Test
    @DisplayName("만료된 refresh token 은 삭제 후 REFRESH_TOKEN_EXPIRED 를 던진다")
    void refresh_expired() {
        RefreshToken refreshToken = RefreshToken.issue(
            1L,
            "session-id-1",
            "refresh-key",
            "hashed-secret",
            LocalDateTime.now(clock).minusMinutes(1),
            LocalDateTime.now(clock),
            null,
            null
        );
        given(refreshTokenRepository.findByTokenKeyForUpdate("refresh-key")).willReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.refresh(new RefreshCommand(null, "refresh-key.refresh-secret")))
            .extracting("errorCode")
            .isEqualTo(REFRESH_TOKEN_EXPIRED);

        then(refreshTokenRepository).should().delete(refreshToken);
    }

    @Test
    @DisplayName("access token 주체와 refresh token 소유자가 다르면 ACCESS_TOKEN_INVALID 를 던진다")
    void refresh_accessTokenOwnerMismatch() {
        RefreshToken refreshToken = RefreshToken.issue(
            1L,
            "session-id-1",
            "refresh-key",
            "hashed-secret",
            LocalDateTime.now(clock).plusHours(1),
            LocalDateTime.now(clock),
            null,
            null
        );
        given(refreshTokenRepository.findByTokenKeyForUpdate("refresh-key")).willReturn(Optional.of(refreshToken));
        given(authTokenProvider.getUserIdFromExpiredToken("expired-access-token")).willReturn(2L);

        assertThatThrownBy(() -> refreshTokenService.refresh(new RefreshCommand("expired-access-token", "refresh-key.refresh-secret")))
            .extracting("errorCode")
            .isEqualTo(ACCESS_TOKEN_INVALID);
    }

    @Test
    @DisplayName("형식이 잘못된 refresh token 은 REFRESH_TOKEN_INVALID 를 던진다")
    void refresh_invalidFormat() {
        assertThatThrownBy(() -> refreshTokenService.refresh(new RefreshCommand(null, "invalid-format")))
            .extracting("errorCode")
            .isEqualTo(REFRESH_TOKEN_INVALID);
    }
}
