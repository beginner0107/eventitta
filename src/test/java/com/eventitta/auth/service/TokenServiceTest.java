package com.eventitta.auth.service;

import com.eventitta.auth.domain.RefreshToken;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.auth.service.dto.TokenResult;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.eventitta.user.domain.Role.USER;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @InjectMocks
    private TokenService tokenService;

    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private Pbkdf2PasswordEncoder pbkdf2PasswordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private Clock clock;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    @Nested
    @DisplayName("토큰 발급")
    class IssueTokens {

        @Test
        @DisplayName("존재하는 회원이면 액세스 토큰과 리프레시 토큰을 발급하고 리프레시 토큰을 저장한다.")
        void issueTokens_whenUserExists_thenReturnsTokensAndStoresRefreshToken() {
            // given
            Long userId = 1L;
            User user = createUser(userId, "spring@test.com");

            given(userRepository.findById(userId)).willReturn(of(user));
            given(tokenProvider.createAccessToken(userId, user.getEmail(), user.getRole().name()))
                .willReturn("access-token");
            given(tokenProvider.createRefreshToken()).willReturn("raw-refresh-token");
            given(pbkdf2PasswordEncoder.encode("raw-refresh-token")).willReturn("encoded-refresh-token");

            Instant expectedExpiry = Instant.parse("2026-03-11T14:59:00Z");
            ZoneId applicationZone = ZoneId.of("Asia/Seoul");
            given(clock.getZone()).willReturn(applicationZone);
            given(tokenProvider.getRefreshTokenExpiry()).willReturn(expectedExpiry);

            // when
            TokenResult result = tokenService.issueTokens(userId);

            // then
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("raw-refresh-token");

            then(refreshTokenRepository).should().save(refreshTokenCaptor.capture());

            RefreshToken savedToken = refreshTokenCaptor.getValue();
            assertThat(savedToken.getTokenHash()).isEqualTo("encoded-refresh-token");
            assertThat(savedToken.getExpiresAt())
                .isEqualTo(LocalDateTime.ofInstant(expectedExpiry, applicationZone));
        }

    }

    private User createUser(Long id, String email) {
        User user = User.builder()
            .email(email)
            .password("encoded-password")
            .nickname("spring")
            .role(USER)
            .build();

        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
