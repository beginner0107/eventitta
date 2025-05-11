package com.eventitta.auth.service;

import com.eventitta.auth.domain.RefreshToken;
import com.eventitta.auth.dto.response.TokenResponse;
import com.eventitta.auth.exception.AuthErrorCode;
import com.eventitta.auth.exception.AuthException;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("리프레시 토큰 서비스 단위 테스트")
class RefreshTokenServiceUnitTest {

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

    private final String ERROR_CODE_FIELD = "errorCode";

    @Test
    @DisplayName("리프레시 토큰이 누락되면 요청이 거부된다")
    void missingRawRefreshToken_throwsMissingException() {
        assertThatThrownBy(() -> refreshTokenService.refresh("anyExpiredAt", null))
            .isInstanceOf(AuthException.class)
            .extracting(ERROR_CODE_FIELD)
            .isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISSING);
    }

    @Test
    @DisplayName("저장된 토큰이 없으면 인증에 실패한다")
    void noStoredRefreshToken_throwsInvalidException() {
        given(tokenProvider.getUserIdFromExpiredToken("expiredAt")).willReturn(10L);
        given(rtRepo.findByUserId(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.refresh("expiredAt", "rawRt"))
            .isInstanceOf(AuthException.class)
            .extracting(ERROR_CODE_FIELD)
            .isEqualTo(AuthErrorCode.REFRESH_TOKEN_INVALID);
    }

    @Test
    @DisplayName("토큰의 해시가 일치하지 않으면 인증에 실패한다")
    void mismatchedRefreshToken_throwsInvalidException() {
        given(tokenProvider.getUserIdFromExpiredToken("expiredAt")).willReturn(20L);
        RefreshToken entity = mock(RefreshToken.class);
        given(rtRepo.findByUserId(20L)).willReturn(Optional.of(entity));
        given(rtEncoder.matches("rawRt", entity.getTokenHash())).willReturn(false);

        assertThatThrownBy(() -> refreshTokenService.refresh("expiredAt", "rawRt"))
            .isInstanceOf(AuthException.class)
            .extracting(ERROR_CODE_FIELD)
            .isEqualTo(AuthErrorCode.REFRESH_TOKEN_INVALID);
    }

    @Test
    @DisplayName("만료된 토큰으로 재발급을 시도하면 거부된다")
    void expiredRefreshToken_throwsExpiredException() {
        given(tokenProvider.getUserIdFromExpiredToken("expiredAt")).willReturn(30L);
        RefreshToken entity = mock(RefreshToken.class);
        given(rtRepo.findByUserId(30L)).willReturn(Optional.of(entity));
        given(rtEncoder.matches("rawRt", entity.getTokenHash())).willReturn(true);
        given(entity.getExpiresAt()).willReturn(LocalDateTime.now().minusSeconds(1));

        assertThatThrownBy(() -> refreshTokenService.refresh("expiredAt", "rawRt"))
            .isInstanceOf(AuthException.class)
            .extracting(ERROR_CODE_FIELD)
            .isEqualTo(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("유효한 리프레시 토큰이면 새 액세스/리프레시 토큰을 발급한다")
    void validRefreshToken_returnsNewTokenResponse() {
        given(tokenProvider.getUserIdFromExpiredToken("expiredAt")).willReturn(40L);
        RefreshToken entity = mock(RefreshToken.class);
        given(rtRepo.findByUserId(40L)).willReturn(Optional.of(entity));
        given(rtEncoder.matches("rawRt", entity.getTokenHash())).willReturn(true);
        given(entity.getExpiresAt()).willReturn(LocalDateTime.now().plusSeconds(60));

        TokenResponse expected = new TokenResponse("newAt", "newRt");
        given(tokenService.issueTokens(40L)).willReturn(expected);

        TokenResponse actual = refreshTokenService.refresh("expiredAt", "rawRt");

        assertThat(actual).isSameAs(expected);
        then(tokenService).should().issueTokens(40L);
    }
}
