package com.eventitta.auth.service;

import com.eventitta.auth.domain.RefreshToken;
import com.eventitta.auth.dto.response.TokenResponse;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("토큰 서비스 단위 테스트")
class TokenServiceUnitTest {

    @InjectMocks
    private TokenService tokenService;

    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private RefreshTokenRepository rtRepo;
    @Mock
    private Pbkdf2PasswordEncoder rtEncoder;
    @Mock
    private UserRepository userRepository;

    @Captor
    ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    @Test
    @DisplayName("리프레시 토큰이 없으면 새 엔티티를 저장하고 액세스·리프레시 토큰을 반환한다")
    void givenNoExistingToken_whenIssueTokens_thenSavesNewRefreshTokenAndReturnsTokens() {
        // given
        Long userId = 55L;
        String rawRt = "rawRefresh";
        String encodedHash = "hashedRt";
        Instant expiryInstant = Instant.now().plusSeconds(3600);
        LocalDateTime expectedExpiry = LocalDateTime.ofInstant(expiryInstant, ZoneId.systemDefault());
        String accessToken = "access123";

        given(tokenProvider.createAccessToken(userId, "test@example.com", "USER")).willReturn(accessToken);
        given(tokenProvider.createRefreshToken()).willReturn(rawRt);
        given(tokenProvider.getRefreshTokenExpiry()).willReturn(expiryInstant);
        given(rtEncoder.encode(rawRt)).willReturn(encodedHash);

        User user = User.builder().id(userId).email("test@example.com").role(Role.USER).build();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.getReferenceById(userId)).willReturn(user);

        // when
        TokenResponse response = tokenService.issueTokens(userId);

        // then
        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(rawRt);

        // then
        then(rtRepo).should().save(refreshTokenCaptor.capture());
        RefreshToken saved = refreshTokenCaptor.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(userId);
        assertThat(saved.getTokenHash()).isEqualTo(encodedHash);
        assertThat(saved.getExpiresAt()).isEqualTo(expectedExpiry);
    }

    @Test
    @DisplayName("새 리프레시 토큰을 생성하여 저장하고 액세스·리프레시 토큰을 반환한다")
    void givenExistingToken_whenIssueTokens_thenCreatesNewRefreshTokenAndReturnsTokens() {
        // given
        Long userId = 77L;
        String rawRt = "rawRt2";
        String encodedHash = "hashedRt2";
        Instant expiryInstant = Instant.now().plusSeconds(7200);
        LocalDateTime expectedExpiry = LocalDateTime.ofInstant(expiryInstant, ZoneId.systemDefault());
        String accessToken = "access456";

        given(tokenProvider.createAccessToken(userId, "admin@example.com", "ADMIN")).willReturn(accessToken);
        given(tokenProvider.createRefreshToken()).willReturn(rawRt);
        given(tokenProvider.getRefreshTokenExpiry()).willReturn(expiryInstant);
        given(rtEncoder.encode(rawRt)).willReturn(encodedHash);

        User user = User.builder().id(userId).email("admin@example.com").role(Role.ADMIN).build();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.getReferenceById(userId)).willReturn(user);

        // when
        TokenResponse response = tokenService.issueTokens(userId);

        // then
        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(rawRt);

        // then
        then(rtRepo).should().save(refreshTokenCaptor.capture());
        RefreshToken saved = refreshTokenCaptor.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(userId);
        assertThat(saved.getTokenHash()).isEqualTo(encodedHash);
        assertThat(saved.getExpiresAt()).isEqualTo(expectedExpiry);
    }
}
