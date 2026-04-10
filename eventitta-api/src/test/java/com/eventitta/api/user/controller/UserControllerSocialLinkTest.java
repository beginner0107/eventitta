package com.eventitta.api.user.controller;

import com.eventitta.api.auth.security.config.SecurityConfig;
import com.eventitta.api.auth.AuthConstants;
import com.eventitta.domain.auth.dto.request.SocialLoginRequest;
import com.eventitta.api.auth.security.principal.UserPrincipal;
import com.eventitta.api.auth.security.handler.JwtAccessDeniedHandler;
import com.eventitta.api.auth.security.handler.JwtAuthenticationEntryPoint;
import com.eventitta.api.auth.jwt.JwtTokenProvider;
import com.eventitta.api.auth.security.userdetails.CustomUserDetailsService;
import com.eventitta.api.common.logging.RequestActorResolver;
import com.eventitta.api.auth.security.config.SecurityCorsProperties;
import com.eventitta.api.auth.cookie.CookieManager;
import com.eventitta.api.auth.oauth.kakao.KakaoAuthorizationSupport;
import com.eventitta.api.user.mapper.UserMapper;
import com.eventitta.domain.auth.exception.AuthErrorCode;
import com.eventitta.domain.auth.repository.RefreshTokenRepository;
import com.eventitta.domain.auth.service.AuthService;
import com.eventitta.domain.auth.dto.AuthSessionMetadata;
import com.eventitta.domain.auth.dto.KakaoLinkCommand;
import com.eventitta.domain.gamification.api.internal.facade.GamificationQueryFacade;
import com.eventitta.domain.notification.domain.AlertLevel;
import com.eventitta.domain.notification.resolver.AlertLevelResolver;
import com.eventitta.domain.notification.service.AlertNotificationService;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import com.eventitta.domain.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class UserControllerSocialLinkTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private GamificationQueryFacade gamificationQueryFacade;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private KakaoAuthorizationSupport kakaoAuthorizationSupport;

    @MockitoBean
    private CookieManager cookieManager;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @MockitoBean
    private RequestActorResolver requestActorResolver;

    @MockitoBean
    private SecurityCorsProperties securityCorsProperties;

    @MockitoBean
    private UserInternalFacade userInternalFacade;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private AlertNotificationService alertNotificationService;

    @MockitoBean
    private AlertLevelResolver alertLevelResolver;

    @BeforeEach
    void setUp() {
        given(securityCorsProperties.getAllowedOrigins()).willReturn(List.of("http://localhost:3000"));
        given(alertLevelResolver.resolveLevel(any())).willReturn(AlertLevel.INFO);
        given(requestActorResolver.resolveActor(any())).willReturn("test-user");
    }

    @Test
    @DisplayName("인증된 사용자가 카카오 연결에 성공하면 state 쿠키를 삭제하고 204를 반환한다")
    void linkKakao_success() throws Exception {
        SocialLoginRequest request = new SocialLoginRequest(
            "authorization-code",
            "oauth-state",
            "http://localhost:3000/auth/kakao/callback"
        );
        org.mockito.Mockito.doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(0);
            response.addHeader(HttpHeaders.SET_COOKIE, "oauth_state=; Max-Age=0; Path=/; HttpOnly");
            return null;
        }).when(cookieManager).deleteCookie(any(HttpServletResponse.class), eq(AuthConstants.OAUTH_STATE));

        mockMvc.perform(post("/api/v1/users/me/social/kakao/link")
                .with(authenticatedUser())
                .cookie(new jakarta.servlet.http.Cookie(AuthConstants.OAUTH_STATE, "oauth-state"))
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent())
            .andExpect(result -> assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(cookie -> {
                    assertThat(cookie).contains("oauth_state=");
                    assertThat(cookie).contains("Max-Age=0");
                }));

        then(kakaoAuthorizationSupport).should().validateState("oauth-state", "oauth-state");
        then(authService).should().linkKakao(1L, new KakaoLinkCommand("authorization-code", "http://localhost:3000/auth/kakao/callback"));
    }

    @Test
    @DisplayName("state 검증이 실패하면 401 응답을 반환한다")
    void linkKakao_invalidState() throws Exception {
        SocialLoginRequest request = new SocialLoginRequest(
            "authorization-code",
            "bad-state",
            "http://localhost:3000/auth/kakao/callback"
        );
        willThrow(AuthErrorCode.OAUTH_STATE_INVALID.defaultException())
            .given(kakaoAuthorizationSupport).validateState(null, "bad-state");
        org.mockito.Mockito.doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(0);
            response.addHeader(HttpHeaders.SET_COOKIE, "oauth_state=; Max-Age=0; Path=/; HttpOnly");
            return null;
        }).when(cookieManager).deleteCookie(any(HttpServletResponse.class), eq(AuthConstants.OAUTH_STATE));

        mockMvc.perform(post("/api/v1/users/me/social/kakao/link")
                .with(authenticatedUser())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("OAUTH_STATE_INVALID"));
    }

    @Test
    @DisplayName("인증된 사용자는 자신의 세션 목록을 조회할 수 있다")
    void getMySessions_success() throws Exception {
        given(authService.getSessions(1L, "session-current")).willReturn(List.of(
            new AuthSessionMetadata(
                "session-current",
                LocalDateTime.of(2026, 3, 27, 10, 0),
                LocalDateTime.of(2026, 3, 27, 10, 5),
                LocalDateTime.of(2026, 3, 28, 10, 0),
                "192.168.0.0/24",
                "Mozilla/5.0",
                "192.168.0.0/24",
                "Mozilla/5.0",
                true
            )
        ));

        mockMvc.perform(get("/api/v1/users/me/sessions")
                .with(authenticatedUser("session-current")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sessionId").value("session-current"))
            .andExpect(jsonPath("$[0].current").value(true));
    }

    @Test
    @DisplayName("현재 세션을 종료하면 토큰 쿠키를 삭제한다")
    void revokeCurrentSession_deletesTokenCookies() throws Exception {
        given(authService.revokeSession(1L, "session-current", "session-current")).willReturn(true);
        org.mockito.Mockito.doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(0);
            response.addHeader(HttpHeaders.SET_COOKIE, "access_token=; Max-Age=0; Path=/; HttpOnly");
            response.addHeader(HttpHeaders.SET_COOKIE, "refresh_token=; Max-Age=0; Path=/api/v1/auth; HttpOnly");
            return null;
        }).when(cookieManager).deleteTokenCookies(any(HttpServletResponse.class));

        mockMvc.perform(delete("/api/v1/users/me/sessions/session-current")
                .with(authenticatedUser("session-current")))
            .andExpect(status().isNoContent())
            .andExpect(result -> assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(cookie -> assertThat(cookie).contains("access_token=").contains("Max-Age=0")));
    }

    private RequestPostProcessor authenticatedUser() {
        return authenticatedUser(null);
    }

    private RequestPostProcessor authenticatedUser(String sessionId) {
        UserPrincipal principal = new UserPrincipal(1L, "user@test.com", "USER", 0L, sessionId);
        return SecurityMockMvcRequestPostProcessors.authentication(
            UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities())
        );
    }
}
