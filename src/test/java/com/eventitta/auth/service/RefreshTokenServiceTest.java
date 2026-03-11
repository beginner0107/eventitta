package com.eventitta.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.anyLong;

import java.time.LocalDateTime;
import java.util.List;

import com.eventitta.auth.domain.RefreshToken;
import com.eventitta.auth.exception.AuthException;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.auth.service.dto.RefreshCommand;
import com.eventitta.auth.service.dto.TokenResult;
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

    @Nested
    @DisplayName("토큰 재발급")
    class Refresh {

        @Test
        @DisplayName("액세스 토큰이 비어 있으면 토큰을 재발급할 수 없다.")
        void refresh_whenAccessTokenIsBlank_thenThrowsException() {
            // given
            RefreshCommand command = new RefreshCommand(" ", "refresh-token");

            // when // then
            assertThatThrownBy(() -> refreshTokenService.refresh(command))
                .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("리프레시 토큰이 비어 있으면 토큰을 재발급할 수 없다.")
        void refresh_whenRefreshTokenIsBlank_thenThrowsException() {
            // given
            RefreshCommand command = new RefreshCommand("expired-access-token", " ");

            // when // then
            assertThatThrownBy(() -> refreshTokenService.refresh(command))
                .isInstanceOf(AuthException.class);
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
        @DisplayName("만료된 리프레시 토큰이면 토큰을 재발급할 수 없다.")
        void refresh_whenRefreshTokenIsExpired_thenThrowsException() {
            // given
            Long userId = 1L;
            RefreshCommand command = new RefreshCommand("expired-access-token", "refresh-token");
            RefreshToken tokenEntity = mock(RefreshToken.class);

            given(tokenProvider.getUserIdFromExpiredToken(command.accessToken())).willReturn(userId);
            given(rtRepo.findAllByUserId(userId)).willReturn(List.of(tokenEntity));
            given(tokenEntity.getTokenHash()).willReturn("stored-hash");
            given(rtEncoder.matches(command.refreshToken(), "stored-hash")).willReturn(true);
            given(tokenEntity.getExpiresAt()).willReturn(LocalDateTime.now().minusMinutes(1));

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
            TokenResult reissuedTokens = new TokenResult("new-access-token", "new-refresh-token");

            given(tokenProvider.getUserIdFromExpiredToken(command.accessToken())).willReturn(userId);
            given(rtRepo.findAllByUserId(userId)).willReturn(List.of(tokenEntity));
            given(tokenEntity.getTokenHash()).willReturn("stored-hash");
            given(rtEncoder.matches(command.refreshToken(), "stored-hash")).willReturn(true);
            given(tokenEntity.getExpiresAt()).willReturn(LocalDateTime.now().plusMinutes(30));
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
    class InvalidateByAccessToken {

        @Test
        @DisplayName("액세스 토큰에서 회원 식별자를 추출해 해당 회원의 리프레시 토큰을 모두 삭제한다.")
        void invalidateByAccessToken_whenAccessTokenIsProvided_thenDeletesAllRefreshTokensOfUser() {
            // given
            String accessToken = "expired-access-token";
            Long userId = 1L;
            given(tokenProvider.getUserIdFromExpiredToken(accessToken)).willReturn(userId);

            // when
            refreshTokenService.invalidateByAccessToken(accessToken);

            // then
            then(rtRepo).should().deleteByUserId(userId);
        }
    }
}
