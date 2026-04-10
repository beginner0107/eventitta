package com.eventitta.api.auth.controller;

import com.eventitta.api.auth.security.config.SecurityConfig;
import com.eventitta.api.auth.AuthConstants;
import com.eventitta.domain.auth.dto.request.ActionTokenRequest;
import com.eventitta.domain.auth.dto.request.EmailRequest;
import com.eventitta.domain.auth.dto.request.PasswordResetConfirmRequest;
import com.eventitta.domain.auth.dto.request.SignInRequest;
import com.eventitta.domain.auth.dto.request.SignUpRequest;
import com.eventitta.domain.auth.dto.request.SocialAuthorizeRequest;
import com.eventitta.domain.auth.dto.request.SocialLoginRequest;
import com.eventitta.api.auth.security.handler.JwtAccessDeniedHandler;
import com.eventitta.api.auth.security.handler.JwtAuthenticationEntryPoint;
import com.eventitta.api.auth.jwt.JwtTokenProvider;
import com.eventitta.api.auth.security.userdetails.CustomUserDetailsService;
import com.eventitta.api.common.logging.RequestActorResolver;
import com.eventitta.api.auth.security.config.SecurityCorsProperties;
import com.eventitta.api.auth.session.ClientSessionMetadataResolver;
import com.eventitta.api.auth.cookie.CookieManager;
import com.eventitta.api.auth.oauth.kakao.KakaoAuthorizationSupport;
import com.eventitta.domain.auth.exception.AuthErrorCode;
import com.eventitta.domain.auth.repository.RefreshTokenRepository;
import com.eventitta.domain.auth.service.AuthService;
import com.eventitta.domain.auth.dto.ClientSessionMetadata;
import com.eventitta.domain.auth.dto.KakaoLoginCommand;
import com.eventitta.domain.auth.dto.RefreshCommand;
import com.eventitta.domain.auth.dto.SignInCommand;
import com.eventitta.domain.auth.dto.SignUpCommand;
import com.eventitta.domain.auth.dto.SignUpResult;
import com.eventitta.domain.auth.dto.TokenResult;
import com.eventitta.domain.notification.domain.AlertLevel;
import com.eventitta.domain.notification.resolver.AlertLevelResolver;
import com.eventitta.domain.notification.service.AlertNotificationService;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import com.eventitta.domain.user.exception.UserErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CookieManager cookieManager;

    @MockitoBean
    private KakaoAuthorizationSupport kakaoAuthorizationSupport;

    @MockitoBean
    private ClientSessionMetadataResolver clientSessionMetadataResolver;

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
    @DisplayName("회원가입 요청을 도메인 auth 서비스로 전달하고 응답 본문을 반환한다")
    void signUp_success() throws Exception {
        SignUpRequest request = new SignUpRequest("test@gmail.com", "password1234!@@", "test123");
        SignUpCommand command = new SignUpCommand("test@gmail.com", "password1234!@@", "test123");
        given(authService.signUp(command)).willReturn(new SignUpResult("test@gmail.com", "test123"));

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@gmail.com"))
            .andExpect(jsonPath("$.nickname").value("test123"));

        then(authService).should().signUp(command);
    }

    @Test
    @DisplayName("로컬 로그인 성공 시 토큰 쿠키를 응답에 추가한다")
    void login_success() throws Exception {
        SignInRequest request = new SignInRequest("test@gmail.com", "password1234!@@");
        ClientSessionMetadata sessionMetadata = new ClientSessionMetadata(
            "192.168.0.0/24",
            "Mozilla/5.0",
            Instant.parse("2026-03-27T10:00:00Z")
        );
        SignInCommand command = new SignInCommand("test@gmail.com", "password1234!@@", sessionMetadata);
        TokenResult tokens = new TokenResult("access-token", "refresh-key.refresh-secret");

        given(clientSessionMetadataResolver.resolve(any())).willReturn(sessionMetadata);
        given(authService.login(command)).willReturn(tokens);
        Mockito.doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(0);
            response.addHeader(HttpHeaders.SET_COOKIE, "access_token=access-token; Path=/; HttpOnly");
            response.addHeader(HttpHeaders.SET_COOKIE, "refresh_token=refresh-key.refresh-secret; Path=/; HttpOnly");
            return null;
        }).when(cookieManager).addTokenCookies(any(HttpServletResponse.class), eq(tokens));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(result -> {
                List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
                assertThat(setCookies).anySatisfy(cookie -> assertThat(cookie).contains("access_token=access-token"));
                assertThat(setCookies).anySatisfy(cookie -> assertThat(cookie).contains("refresh_token=refresh-key.refresh-secret"));
            });
    }

    @Test
    @DisplayName("카카오 인가 URL 발급 시 state 쿠키와 authorize URL을 내려준다")
    void authorizeKakao_success() throws Exception {
        SocialAuthorizeRequest request = new SocialAuthorizeRequest("http://localhost:3000/auth/kakao/callback");
        given(kakaoAuthorizationSupport.generateState()).willReturn("oauth-state");
        given(kakaoAuthorizationSupport.buildAuthorizeUrl(request.redirectUri(), "oauth-state"))
            .willReturn("https://kauth.kakao.com/oauth/authorize?state=oauth-state");
        Mockito.doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(0);
            response.addHeader(HttpHeaders.SET_COOKIE, "oauth_state=oauth-state; Path=/; HttpOnly");
            return null;
        }).when(cookieManager).addShortLivedCookie(any(HttpServletResponse.class), eq(AuthConstants.OAUTH_STATE), eq("oauth-state"));

        mockMvc.perform(post("/api/v1/auth/social/kakao/authorize")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("oauth_state=oauth-state")))
            .andExpect(jsonPath("$.authorizeUrl").value("https://kauth.kakao.com/oauth/authorize?state=oauth-state"));
    }

    @Test
    @DisplayName("카카오 로그인 성공 시 state 쿠키를 삭제하고 서비스 토큰 쿠키를 발급한다")
    void loginWithKakao_success() throws Exception {
        SocialLoginRequest request = new SocialLoginRequest(
            "authorization-code",
            "oauth-state",
            "http://localhost:3000/auth/kakao/callback"
        );
        ClientSessionMetadata sessionMetadata = new ClientSessionMetadata(
            "192.168.0.0/24",
            "Mozilla/5.0",
            Instant.parse("2026-03-27T10:00:00Z")
        );
        TokenResult tokens = new TokenResult("access-token", "refresh-key.refresh-secret");

        given(clientSessionMetadataResolver.resolve(any())).willReturn(sessionMetadata);
        given(authService.loginWithKakao(new KakaoLoginCommand(
            "authorization-code",
            "http://localhost:3000/auth/kakao/callback",
            sessionMetadata
        )))
            .willReturn(tokens);
        Mockito.doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(0);
            response.addHeader(HttpHeaders.SET_COOKIE, "access_token=access-token; Path=/; HttpOnly");
            response.addHeader(HttpHeaders.SET_COOKIE, "refresh_token=refresh-key.refresh-secret; Path=/; HttpOnly");
            return null;
        }).when(cookieManager).addTokenCookies(any(HttpServletResponse.class), eq(tokens));
        Mockito.doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(0);
            response.addHeader(HttpHeaders.SET_COOKIE, "oauth_state=; Max-Age=0; Path=/; HttpOnly");
            return null;
        }).when(cookieManager).deleteCookie(any(HttpServletResponse.class), eq(AuthConstants.OAUTH_STATE));

        mockMvc.perform(post("/api/v1/auth/social/kakao/login")
                .contentType(APPLICATION_JSON)
                .cookie(new jakarta.servlet.http.Cookie(AuthConstants.OAUTH_STATE, "oauth-state"))
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(result -> {
                List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
                assertThat(setCookies).anySatisfy(cookie -> assertThat(cookie).contains("access_token=access-token"));
                assertThat(setCookies).anySatisfy(cookie -> assertThat(cookie).contains("refresh_token=refresh-key.refresh-secret"));
                assertThat(setCookies).anySatisfy(cookie -> {
                    assertThat(cookie).contains("oauth_state=");
                    assertThat(cookie).contains("Max-Age=0");
                });
            });

        then(kakaoAuthorizationSupport).should().validateState("oauth-state", "oauth-state");
    }

    @Test
    @DisplayName("refresh 성공 시 세션 메타데이터를 포함해 auth 서비스에 전달한다")
    void refresh_success() throws Exception {
        ClientSessionMetadata sessionMetadata = new ClientSessionMetadata(
            "10.0.0.0/24",
            "Chrome/123.0",
            Instant.parse("2026-03-27T10:05:00Z")
        );
        TokenResult tokens = new TokenResult("rotated-access-token", "new-key.new-secret");

        given(clientSessionMetadataResolver.resolve(any())).willReturn(sessionMetadata);
        given(authService.refresh(new RefreshCommand("access-token", "refresh-key.refresh-secret", sessionMetadata)))
            .willReturn(tokens);
        Mockito.doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(0);
            response.addHeader(HttpHeaders.SET_COOKIE, "access_token=rotated-access-token; Path=/; HttpOnly");
            response.addHeader(HttpHeaders.SET_COOKIE, "refresh_token=new-key.new-secret; Path=/; HttpOnly");
            return null;
        }).when(cookieManager).addTokenCookies(any(HttpServletResponse.class), eq(tokens));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie(AuthConstants.ACCESS_TOKEN, "access-token"))
                .cookie(new jakarta.servlet.http.Cookie(AuthConstants.REFRESH_TOKEN, "refresh-key.refresh-secret")))
            .andExpect(status().isOk())
            .andExpect(result -> {
                List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
                assertThat(setCookies).anySatisfy(cookie -> assertThat(cookie).contains("access_token=rotated-access-token"));
                assertThat(setCookies).anySatisfy(cookie -> assertThat(cookie).contains("refresh_token=new-key.new-secret"));
            });
    }

    @Test
    @DisplayName("카카오 로그인 state 검증이 실패하면 401 응답을 반환한다")
    void loginWithKakao_invalidState() throws Exception {
        SocialLoginRequest request = new SocialLoginRequest(
            "authorization-code",
            "bad-state",
            "http://localhost:3000/auth/kakao/callback"
        );
        willThrow(AuthErrorCode.OAUTH_STATE_INVALID.defaultException())
            .given(kakaoAuthorizationSupport).validateState(null, "bad-state");
        Mockito.doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(0);
            response.addHeader(HttpHeaders.SET_COOKIE, "oauth_state=; Max-Age=0; Path=/; HttpOnly");
            return null;
        }).when(cookieManager).deleteCookie(any(HttpServletResponse.class), eq(AuthConstants.OAUTH_STATE));

        mockMvc.perform(post("/api/v1/auth/social/kakao/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("OAUTH_STATE_INVALID"))
            .andExpect(result -> assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(cookie -> {
                    assertThat(cookie).contains("oauth_state=");
                    assertThat(cookie).contains("Max-Age=0");
                }));
    }

    @Test
    @DisplayName("회원가입 이메일 중복 예외는 409로 매핑된다")
    void signUp_duplicateEmail() throws Exception {
        SignUpRequest request = new SignUpRequest("test@gmail.com", "password1234!@@", "test123");
        given(authService.signUp(any(SignUpCommand.class))).willThrow(UserErrorCode.CONFLICTED_EMAIL.defaultException());

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("CONFLICTED_EMAIL"));

        ArgumentCaptor<SignUpCommand> captor = ArgumentCaptor.forClass(SignUpCommand.class);
        then(authService).should().signUp(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("test@gmail.com");
    }

    @Test
    @DisplayName("이메일 인증 재발송 요청은 세션 메타데이터와 함께 auth 서비스에 전달된다")
    void requestEmailVerification_success() throws Exception {
        ClientSessionMetadata sessionMetadata = new ClientSessionMetadata(
            "10.0.0.0/24",
            "Chrome/123.0",
            Instant.parse("2026-03-27T10:15:00Z")
        );
        given(clientSessionMetadataResolver.resolve(any())).willReturn(sessionMetadata);

        mockMvc.perform(post("/api/v1/auth/email-verification/request")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EmailRequest("verify@test.com"))))
            .andExpect(status().isNoContent());

        then(authService).should().requestEmailVerification("verify@test.com", sessionMetadata);
    }

    @Test
    @DisplayName("비밀번호 재설정 완료 요청은 204를 반환한다")
    void confirmPasswordReset_success() throws Exception {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("reset.token", "NewPw123!");

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        then(authService).should().confirmPasswordReset("reset.token", "NewPw123!");
    }

    @Test
    @DisplayName("이메일 인증 완료 요청은 204를 반환한다")
    void confirmEmailVerification_success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/email-verification/confirm")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ActionTokenRequest("verify.token"))))
            .andExpect(status().isNoContent());

        then(authService).should().confirmEmailVerification("verify.token");
    }
}
