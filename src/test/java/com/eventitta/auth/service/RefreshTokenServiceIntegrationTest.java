package com.eventitta.auth.service;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.auth.domain.RefreshToken;
import com.eventitta.auth.exception.AuthErrorCode;
import com.eventitta.auth.exception.AuthException;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.auth.service.dto.RefreshCommand;
import com.eventitta.auth.service.dto.TokenResult;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("리프레시 토큰 서비스 통합 테스트")
@Transactional
class RefreshTokenServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private Pbkdf2PasswordEncoder refreshTokenEncoder;

    @Autowired
    private Clock clock;

    @Test
    @DisplayName("유효한 리프레시 토큰이면 기존 토큰을 제거하고 새 토큰을 재발급한다")
    void refresh_reissuesTokensAndReplacesStoredRefreshToken() {
        // given
        User user = userRepository.saveAndFlush(createUser("refresh-success@test.com", "refreshUser"));
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String rawRefreshToken = "refresh-token-value";
        RefreshToken savedToken = refreshTokenRepository.saveAndFlush(new RefreshToken(
            user,
            refreshTokenEncoder.encode(rawRefreshToken),
            LocalDateTime.now(clock).plusDays(1)
        ));

        // when
        TokenResult result = refreshTokenService.refresh(new RefreshCommand(accessToken, rawRefreshToken));

        // then
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(jwtTokenProvider.getUserId(result.accessToken())).isEqualTo(user.getId());

        List<RefreshToken> refreshTokens = refreshTokenRepository.findAllByUserId(user.getId());
        assertThat(refreshTokens).hasSize(1);
        assertThat(refreshTokens).extracting(RefreshToken::getId).doesNotContain(savedToken.getId());

        RefreshToken reissuedToken = refreshTokens.get(0);
        assertThat(refreshTokenEncoder.matches(result.refreshToken(), reissuedToken.getTokenHash())).isTrue();
    }

    @Test
    @DisplayName("리프레시 토큰이 비어 있으면 재발급을 거부한다")
    void refresh_throwsWhenRefreshTokenIsBlank() {
        // given
        User user = userRepository.saveAndFlush(createUser("refresh-blank@test.com", "refreshBlank"));
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());

        // when // then
        assertThatThrownBy(() -> refreshTokenService.refresh(new RefreshCommand(accessToken, " ")))
            .isInstanceOf(AuthException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISSING);
    }

    @Test
    @DisplayName("access token 이 없으면 재발급을 거부한다")
    void refresh_throwsWhenAccessTokenIsMissing() {
        // when // then
        assertThatThrownBy(() -> refreshTokenService.refresh(new RefreshCommand(null, "refresh-token")))
            .isInstanceOf(AuthException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.ACCESS_TOKEN_INVALID);
    }

    @Test
    @DisplayName("손상된 access token 이면 재발급을 거부한다")
    void refresh_throwsWhenAccessTokenIsInvalid() {
        // when // then
        assertThatThrownBy(() -> refreshTokenService.refresh(new RefreshCommand("malformed-access-token", "refresh-token")))
            .isInstanceOf(AuthException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.ACCESS_TOKEN_INVALID);
    }

    @Test
    @DisplayName("저장된 리프레시 토큰과 일치하지 않으면 재발급을 거부한다")
    void refresh_throwsWhenRefreshTokenDoesNotMatch() {
        // given
        User user = userRepository.saveAndFlush(createUser("refresh-invalid@test.com", "refreshInvalid"));
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        refreshTokenRepository.saveAndFlush(new RefreshToken(
            user,
            refreshTokenEncoder.encode("stored-refresh-token"),
            LocalDateTime.now(clock).plusDays(1)
        ));

        // when // then
        assertThatThrownBy(() -> refreshTokenService.refresh(new RefreshCommand(accessToken, "another-refresh-token")))
            .isInstanceOf(AuthException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.REFRESH_TOKEN_INVALID);

        assertThat(refreshTokenRepository.findAllByUserId(user.getId())).hasSize(1);
    }

    @Test
    @DisplayName("만료된 리프레시 토큰이면 재발급을 거부한다")
    void refresh_throwsWhenRefreshTokenExpired() {
        // given
        User user = userRepository.saveAndFlush(createUser("refresh-expired@test.com", "refreshExpired"));
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String rawRefreshToken = "expired-refresh-token";
        refreshTokenRepository.saveAndFlush(new RefreshToken(
            user,
            refreshTokenEncoder.encode(rawRefreshToken),
            LocalDateTime.now(clock).minusSeconds(1)
        ));

        // when // then
        assertThatThrownBy(() -> refreshTokenService.refresh(new RefreshCommand(accessToken, rawRefreshToken)))
            .isInstanceOf(AuthException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("로그아웃 무효화 시 일치하는 리프레시 토큰만 삭제한다")
    void invalidateByToken_deletesOnlyMatchedRefreshToken() {
        // given
        User user = userRepository.saveAndFlush(createUser("refresh-invalidate@test.com", "refreshInvalidate"));
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());

        RefreshToken otherToken = refreshTokenRepository.saveAndFlush(new RefreshToken(
            user,
            refreshTokenEncoder.encode("other-refresh-token"),
            LocalDateTime.now(clock).plusDays(1)
        ));
        RefreshToken matchedToken = refreshTokenRepository.saveAndFlush(new RefreshToken(
            user,
            refreshTokenEncoder.encode("matched-refresh-token"),
            LocalDateTime.now(clock).plusDays(1)
        ));

        // when
        refreshTokenService.invalidateByToken(accessToken, "matched-refresh-token");

        // then
        assertThat(refreshTokenRepository.findById(otherToken.getId())).isPresent();
        assertThat(refreshTokenRepository.findById(matchedToken.getId())).isEmpty();
    }

    private User createUser(String email, String nickname) {
        return User.builder()
            .email(email)
            .password("encoded-password")
            .nickname(nickname)
            .provider(Provider.LOCAL)
            .role(Role.USER)
            .build();
    }
}
