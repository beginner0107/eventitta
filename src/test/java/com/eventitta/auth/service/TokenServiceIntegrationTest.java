package com.eventitta.auth.service;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.auth.domain.RefreshToken;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.auth.service.dto.TokenResult;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.exception.UserErrorCode;
import com.eventitta.user.exception.UserException;
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

@DisplayName("토큰 발급 서비스 통합 테스트")
@Transactional
class TokenServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private Pbkdf2PasswordEncoder refreshTokenEncoder;

    @Autowired
    private Clock clock;

    @Test
    @DisplayName("존재하는 회원이면 토큰을 발급하고 해시된 리프레시 토큰을 저장한다")
    void issueTokens_persistsHashedRefreshToken() {
        // given
        User user = userRepository.saveAndFlush(createUser("token-issue@test.com", "tokenUser"));

        // when
        TokenResult result = tokenService.issueTokens(user.getId());

        // then
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(jwtTokenProvider.getUserId(result.accessToken())).isEqualTo(user.getId());

        List<RefreshToken> refreshTokens = refreshTokenRepository.findAllByUserId(user.getId());
        assertThat(refreshTokens).hasSize(1);

        RefreshToken savedRefreshToken = refreshTokens.get(0);
        assertThat(savedRefreshToken.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedRefreshToken.getTokenHash()).isNotEqualTo(result.refreshToken());
        assertThat(refreshTokenEncoder.matches(result.refreshToken(), savedRefreshToken.getTokenHash())).isTrue();
        assertThat(savedRefreshToken.getExpiresAt()).isAfter(LocalDateTime.now(clock));
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 토큰을 발급할 수 없다")
    void issueTokens_throwsWhenUserDoesNotExist() {
        // given
        Long missingUserId = 9_999L;

        // when // then
        assertThatThrownBy(() -> tokenService.issueTokens(missingUserId))
            .isInstanceOf(UserException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.NOT_FOUND_USER_ID);

        assertThat(refreshTokenRepository.findAll()).isEmpty();
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
