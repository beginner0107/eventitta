package com.eventitta.api.auth.jwt;

import com.eventitta.api.auth.security.UserPrincipal;
import com.eventitta.api.auth.jwt.JwtTokenProvider;
import com.eventitta.api.auth.jwt.ParsedAccessToken;
import com.eventitta.domain.auth.domain.RefreshToken;
import com.eventitta.domain.auth.repository.RefreshTokenRepository;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import com.eventitta.domain.user.api.internal.view.UserAuthView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;

import static com.eventitta.api.auth.AuthConstants.AUTHORIZATION_HEADER;
import static com.eventitta.api.auth.AuthConstants.BEARER_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserInternalFacade userInternalFacade;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider, userInternalFacade, refreshTokenRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("access token 은 요청당 한 번만 파싱해 principal 을 만든다")
    void doFilterInternal_parsesAccessTokenOnce() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.addHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + "access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenProvider.parseAccessToken("access-token"))
            .willReturn(new ParsedAccessToken(
                1L,
                "user@test.com",
                "USER",
                2L,
                "session-1",
                Instant.parse("2026-03-27T10:00:00Z"),
                Instant.parse("2026-03-27T11:00:00Z")
            ));
        given(userInternalFacade.findActiveUserById(1L))
            .willReturn(java.util.Optional.of(new UserAuthView(1L, "user@test.com", "encoded", "USER", true, false, 2L, "tester", null)));
        given(refreshTokenRepository.findByUserIdAndSessionId(1L, "session-1"))
            .willReturn(java.util.Optional.of(org.mockito.Mockito.mock(RefreshToken.class)));

        jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.getId()).isEqualTo(1L);
        assertThat(principal.getEmail()).isEqualTo("user@test.com");
        assertThat(principal.getSessionId()).isEqualTo("session-1");
        then(jwtTokenProvider).should().parseAccessToken("access-token");
        then(userInternalFacade).should().findActiveUserById(1L);
        then(refreshTokenRepository).should().findByUserIdAndSessionId(1L, "session-1");
    }
}
