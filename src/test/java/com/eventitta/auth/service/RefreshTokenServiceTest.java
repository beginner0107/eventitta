package com.eventitta.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.anyLong;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.eventitta.auth.domain.RefreshToken;
import com.eventitta.auth.exception.AuthException;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.auth.service.dto.RefreshCommand;
import com.eventitta.auth.service.dto.TokenResult;
import com.eventitta.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private RefreshTokenRepository rtRepo;
    @Mock
    private Pbkdf2PasswordEncoder rtEncoder;
    @Mock
    private TokenService tokenService;
    @Mock
    private Clock clock;

    @Nested
    @DisplayName("토큰 재발급")
    class Refresh {
        private static final Instant FIXED_INSTANT = Instant.parse("2026-03-11T10:15:30Z");
        private static final ZoneId FIXED_ZONE = ZoneId.of("Asia/Seoul");
        private static final LocalDateTime FIXED_NOW = LocalDateTime.ofInstant(FIXED_INSTANT, FIXED_ZONE);

        @Test
        @DisplayName("액세스 토큰이 null 이면 토큰을 재발급할 수 없다.")
        void refresh_whenAccessTokenIsNull_thenThrowsException() {
            // given
            RefreshCommand command = new RefreshCommand(null, "refresh-token");

            // when // then
            assertThatThrownBy(() -> refreshTokenService.refresh(command))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(com.eventitta.auth.exception.AuthErrorCode.ACCESS_TOKEN_INVALID);

            then(tokenProvider).shouldHaveNoInteractions();
            then(rtRepo).shouldHaveNoInteractions();
            then(tokenService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("리프레시 토큰이 비어 있으면 토큰을 재발급할 수 없다.")
        void refresh_whenRefreshTokenIsBlank_thenThrowsException() {
            // given
            RefreshCommand command = new RefreshCommand("expired-access-token", " ");

            // when // then
            assertThatThrownBy(() -> refreshTokenService.refresh(command))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(com.eventitta.auth.exception.AuthErrorCode.REFRESH_TOKEN_MISSING);
        }

        @Test
        @DisplayName("저장된 리프레시 토큰과 일치하지 않으면 토큰을 재발급할 수 없다.")
        void refresh_whenRefreshTokenDoesNotMatch_thenThrowsException() {
            // given
            Long userId = 1L;
            RefreshCommand command = new RefreshCommand("expired-access-token", "refresh-token");
            RefreshToken tokenEntity = mock(RefreshToken.class);

            given(tokenProvider.getUserIdFromExpiredToken(command.accessToken())).willReturn(userId);
            given(rtRepo.findAllByUserId(userId)).willReturn(List.of(tokenEntity));
            given(tokenEntity.getTokenHash()).willReturn("stored-hash");
            given(rtEncoder.matches(command.refreshToken(), "stored-hash")).willReturn(false);

            // when // then
            assertThatThrownBy(() -> refreshTokenService.refresh(command))
                .isInstanceOf(AuthException.class);

            then(rtRepo).should(never()).delete(tokenEntity);
            then(tokenService).should(never()).issueTokens(anyLong());
        }

        @Test
        @DisplayName("액세스 토큰이 손상되면 토큰을 재발급할 수 없다.")
        void refresh_whenAccessTokenIsInvalid_thenThrowsException() {
            // given
            RefreshCommand command = new RefreshCommand("malformed-access-token", "refresh-token");
            given(tokenProvider.getUserIdFromExpiredToken(command.accessToken()))
                .willThrow(com.eventitta.auth.exception.AuthErrorCode.ACCESS_TOKEN_INVALID.defaultException());

            // when // then
            assertThatThrownBy(() -> refreshTokenService.refresh(command))
                .isInstanceOf(AuthException.class);

            then(rtRepo).shouldHaveNoInteractions();
            then(tokenService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("만료된 리프레시 토큰이면 토큰을 재발급할 수 없다.")
        void refresh_whenRefreshTokenIsExpired_thenThrowsException() {
            // given
            Long userId = 1L;
            RefreshCommand command = new RefreshCommand("expired-access-token", "refresh-token");
            RefreshToken tokenEntity = mock(RefreshToken.class);
            given(clock.instant()).willReturn(FIXED_INSTANT);
            given(clock.getZone()).willReturn(FIXED_ZONE);

            given(tokenProvider.getUserIdFromExpiredToken(command.accessToken())).willReturn(userId);
            given(rtRepo.findAllByUserId(userId)).willReturn(List.of(tokenEntity));
            given(tokenEntity.getTokenHash()).willReturn("stored-hash");
            given(rtEncoder.matches(command.refreshToken(), "stored-hash")).willReturn(true);
            given(tokenEntity.getExpiresAt()).willReturn(FIXED_NOW.minusSeconds(1));

            // when // then
            assertThatThrownBy(() -> refreshTokenService.refresh(command))
                .isInstanceOf(AuthException.class);

            then(rtRepo).should(never()).delete(tokenEntity);
            then(tokenService).should(never()).issueTokens(anyLong());
        }

        @Test
        @DisplayName("유효한 리프레시 토큰이면 기존 리프레시 토큰을 삭제하고 토큰을 재발급한다.")
        void refresh_whenRefreshTokenMatchesAndIsNotExpired_thenReissuesTokens() {
            // given
            Long userId = 1L;
            RefreshCommand command = new RefreshCommand("expired-access-token", "refresh-token");
            RefreshToken tokenEntity = mock(RefreshToken.class);
            User user = mock(User.class);
            TokenResult reissuedTokens = new TokenResult("new-access-token", "new-refresh-token");
            given(clock.instant()).willReturn(FIXED_INSTANT);
            given(clock.getZone()).willReturn(FIXED_ZONE);

            given(tokenProvider.getUserIdFromExpiredToken(command.accessToken())).willReturn(userId);
            given(rtRepo.findAllByUserId(userId)).willReturn(List.of(tokenEntity));
            given(tokenEntity.getTokenHash()).willReturn("stored-hash");
            given(rtEncoder.matches(command.refreshToken(), "stored-hash")).willReturn(true);
            given(tokenEntity.getExpiresAt()).willReturn(FIXED_NOW.plusMinutes(30));
            given(tokenEntity.getUser()).willReturn(user);
            given(user.getId()).willReturn(userId);
            given(tokenService.issueTokens(userId)).willReturn(reissuedTokens);

            // when
            TokenResult result = refreshTokenService.refresh(command);

            // then
            assertThat(result).isEqualTo(reissuedTokens);
            then(rtRepo).should().delete(tokenEntity);
            then(tokenService).should().issueTokens(userId);
        }

        @Test
        @DisplayName("만료 시각이 현재와 정확히 같으면 아직 유효한 것으로 처리한다.")
        void refresh_whenRefreshTokenExpiresExactlyNow_thenReissuesTokens() {
            // given
            Long userId = 1L;
            RefreshCommand command = new RefreshCommand("expired-access-token", "refresh-token");
            RefreshToken tokenEntity = mock(RefreshToken.class);
            User user = mock(User.class);
            TokenResult reissuedTokens = new TokenResult("new-access-token", "new-refresh-token");
            given(clock.instant()).willReturn(FIXED_INSTANT);
            given(clock.getZone()).willReturn(FIXED_ZONE);

            given(tokenProvider.getUserIdFromExpiredToken(command.accessToken())).willReturn(userId);
            given(rtRepo.findAllByUserId(userId)).willReturn(List.of(tokenEntity));
            given(tokenEntity.getTokenHash()).willReturn("stored-hash");
            given(rtEncoder.matches(command.refreshToken(), "stored-hash")).willReturn(true);
            given(tokenEntity.getExpiresAt()).willReturn(FIXED_NOW);
            given(tokenEntity.getUser()).willReturn(user);
            given(user.getId()).willReturn(userId);
            given(tokenService.issueTokens(userId)).willReturn(reissuedTokens);

            // when
            TokenResult result = refreshTokenService.refresh(command);

            // then
            assertThat(result).isEqualTo(reissuedTokens);
            then(rtRepo).should().delete(tokenEntity);
            then(tokenService).should().issueTokens(userId);
        }
    }

    @Nested
    @DisplayName("리프레시 토큰 무효화")
    class InvalidateByToken {

        @Test
        @DisplayName("액세스 토큰과 일치하는 리프레시 토큰이 있으면 해당 토큰만 삭제한다.")
        void invalidateByToken_whenMatchingRefreshTokenExists_thenDeletesMatchedToken() {
            // given
            String accessToken = "expired-access-token";
            String refreshToken = "refresh-token";
            Long userId = 1L;
            RefreshToken matchedToken = mock(RefreshToken.class);
            RefreshToken otherToken = mock(RefreshToken.class);

            given(tokenProvider.getUserIdFromExpiredToken(accessToken)).willReturn(userId);
            given(rtRepo.findAllByUserId(userId)).willReturn(List.of(otherToken, matchedToken));
            given(otherToken.getTokenHash()).willReturn("other-hash");
            given(matchedToken.getTokenHash()).willReturn("matched-hash");
            given(rtEncoder.matches(refreshToken, "other-hash")).willReturn(false);
            given(rtEncoder.matches(refreshToken, "matched-hash")).willReturn(true);

            // when
            refreshTokenService.invalidateByToken(accessToken, refreshToken);

            // then
            then(rtRepo).should().delete(matchedToken);
            then(rtRepo).should(never()).delete(otherToken);
        }

        @Test
        @DisplayName("일치하는 리프레시 토큰이 없으면 아무 것도 삭제하지 않는다.")
        void invalidateByToken_whenMatchingRefreshTokenDoesNotExist_thenDeletesNothing() {
            // given
            String accessToken = "expired-access-token";
            String refreshToken = "refresh-token";
            Long userId = 1L;
            RefreshToken storedToken = mock(RefreshToken.class);

            given(tokenProvider.getUserIdFromExpiredToken(accessToken)).willReturn(userId);
            given(rtRepo.findAllByUserId(userId)).willReturn(List.of(storedToken));
            given(storedToken.getTokenHash()).willReturn("stored-hash");
            given(rtEncoder.matches(refreshToken, "stored-hash")).willReturn(false);

            // when
            refreshTokenService.invalidateByToken(accessToken, refreshToken);

            // then
            then(rtRepo).should(never()).delete(any(RefreshToken.class));
        }
    }
}
