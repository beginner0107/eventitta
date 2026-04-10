package com.eventitta.domain.auth.service;

import com.eventitta.domain.auth.domain.RefreshToken;
import com.eventitta.domain.auth.port.AuthTokenProvider;
import com.eventitta.domain.auth.repository.RefreshTokenRepository;
import com.eventitta.domain.auth.service.dto.ClientSessionMetadata;
import com.eventitta.domain.auth.service.dto.TokenResult;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import com.eventitta.domain.user.api.internal.view.UserAuthView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private AuthTokenProvider authTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private Pbkdf2PasswordEncoder refreshTokenEncoder;

    @Mock
    private UserInternalFacade userInternalFacade;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-27T10:00:00Z"), ZoneOffset.UTC);

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(
            authTokenProvider,
            refreshTokenRepository,
            refreshTokenEncoder,
            userInternalFacade,
            clock
        );
    }

    @Test
    @DisplayName("토큰 발급 시 refresh session row 에 메타데이터를 포함해 저장한다")
    void issueTokens_createsSelectorBasedRefreshToken() {
        ClientSessionMetadata sessionMetadata = new ClientSessionMetadata(
            "192.168.10.0/24",
            "Mozilla/5.0",
            Instant.parse("2026-03-27T10:00:10Z")
        );
        given(userInternalFacade.findActiveUserById(1L))
            .willReturn(Optional.of(new UserAuthView(1L, "user@test.com", "encoded", "USER", true, false, 3L, "tester", null)));
        given(authTokenProvider.createAccessToken(1L, "user@test.com", "USER", 3L, anyString())).willReturn("access-token");
        given(authTokenProvider.createRefreshTokenKey()).willReturn("refresh-key");
        given(authTokenProvider.createRefreshTokenSecret()).willReturn("refresh-secret");
        given(authTokenProvider.getRefreshTokenExpiry()).willReturn(Instant.parse("2026-03-28T10:00:00Z"));
        given(refreshTokenEncoder.encode("refresh-secret")).willReturn("hashed-refresh-secret");
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(invocation -> invocation.getArgument(0));

        TokenResult result = tokenService.issueTokens(1L, sessionMetadata);

        assertThat(result.refreshToken()).isEqualTo("refresh-key.refresh-secret");
        assertThat(result.accessToken()).isEqualTo("access-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        then(refreshTokenRepository).should().save(captor.capture());
        assertThat(captor.getValue().getSessionId()).isNotBlank();
        assertThat(captor.getValue().getTokenKey()).isEqualTo("refresh-key");
        assertThat(captor.getValue().getTokenHash()).isEqualTo("hashed-refresh-secret");
        assertThat(captor.getValue().getIssuedIpMasked()).isEqualTo("192.168.10.0/24");
        assertThat(captor.getValue().getIssuedUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(captor.getValue().getLastSeenAt()).isEqualTo(LocalDateTime.ofInstant(sessionMetadata.observedAt(), clock.getZone()));
        assertThat(captor.getValue().getLastSeenIpMasked()).isEqualTo("192.168.10.0/24");
        assertThat(captor.getValue().getLastSeenUserAgent()).isEqualTo("Mozilla/5.0");
    }

    @Test
    @DisplayName("refresh rotation 은 같은 session id 를 유지하고 token key 와 last seen 메타데이터만 갱신한다")
    void rotateTokens_updatesExistingSessionRow() {
        ClientSessionMetadata sessionMetadata = new ClientSessionMetadata(
            "10.0.0.0/24",
            "Chrome/123.0",
            Instant.parse("2026-03-27T10:30:00Z")
        );
        RefreshToken existingSession = RefreshToken.issue(
            1L,
            "session-id-1",
            "old-key",
            "old-hash",
            LocalDateTime.of(2026, 3, 28, 10, 0),
            LocalDateTime.of(2026, 3, 27, 10, 0),
            "192.168.10.0/24",
            "Mozilla/5.0"
        );

        given(userInternalFacade.findActiveUserById(1L))
            .willReturn(Optional.of(new UserAuthView(1L, "user@test.com", "encoded", "USER", true, false, 7L, "tester", null)));
        given(authTokenProvider.createAccessToken(1L, "user@test.com", "USER", 7L, "session-id-1"))
            .willReturn("rotated-access-token");
        given(authTokenProvider.createRefreshTokenKey()).willReturn("new-key");
        given(authTokenProvider.createRefreshTokenSecret()).willReturn("new-secret");
        given(authTokenProvider.getRefreshTokenExpiry()).willReturn(Instant.parse("2026-03-29T10:00:00Z"));
        given(refreshTokenEncoder.encode("new-secret")).willReturn("new-hash");
        given(refreshTokenRepository.save(existingSession)).willReturn(existingSession);

        TokenResult result = tokenService.rotateTokens(existingSession, sessionMetadata);

        assertThat(result.accessToken()).isEqualTo("rotated-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-key.new-secret");
        assertThat(existingSession.getSessionId()).isEqualTo("session-id-1");
        assertThat(existingSession.getTokenKey()).isEqualTo("new-key");
        assertThat(existingSession.getTokenHash()).isEqualTo("new-hash");
        assertThat(existingSession.getLastSeenAt()).isEqualTo(LocalDateTime.ofInstant(sessionMetadata.observedAt(), clock.getZone()));
        assertThat(existingSession.getLastSeenIpMasked()).isEqualTo("10.0.0.0/24");
        assertThat(existingSession.getLastSeenUserAgent()).isEqualTo("Chrome/123.0");
        assertThat(existingSession.getIssuedIpMasked()).isEqualTo("192.168.10.0/24");
        assertThat(existingSession.getIssuedUserAgent()).isEqualTo("Mozilla/5.0");
        then(refreshTokenRepository).should().save(existingSession);
    }
}
