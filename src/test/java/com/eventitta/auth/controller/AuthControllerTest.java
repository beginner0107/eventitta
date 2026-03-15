package com.eventitta.auth.controller;

import com.eventitta.auth.controller.request.SignInRequest;
import com.eventitta.auth.controller.request.SignUpRequest;
import com.eventitta.auth.jwt.service.UserInfoService;
import com.eventitta.auth.service.AuthService;
import com.eventitta.auth.service.dto.LogoutCommand;
import com.eventitta.auth.service.dto.RefreshCommand;
import com.eventitta.auth.service.dto.SignInCommand;
import com.eventitta.auth.service.dto.SignUpCommand;
import com.eventitta.auth.service.dto.SignUpResult;
import com.eventitta.notification.resolver.AlertLevelResolver;
import com.eventitta.notification.service.DiscordNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static com.eventitta.auth.constants.AuthConstants.ACCESS_TOKEN;
import static com.eventitta.auth.constants.AuthConstants.REFRESH_TOKEN;
import static com.eventitta.auth.exception.AuthErrorCode.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private DiscordNotificationService discordNotificationService;

    @MockitoBean
    private AlertLevelResolver alertLevelResolver;

    @MockitoBean
    private UserInfoService userInfoService;

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        @Test
        @DisplayName("유효한 회원가입 요청이면 회원 정보와 함께 200 응답을 반환한다")
        void signUp_success() throws Exception {
            // given
            String email = "test@gmail.com";
            String nickname = "test123";
            String password = "password1234!@@";
            SignUpRequest signupReq = new SignUpRequest(email, password, nickname);

            SignUpResult signUpResult = new SignUpResult(email, nickname);
            given(authService.signUp(signupReq.toCommand())).willReturn(signUpResult);

            // when
            var result = mockMvc.perform(
                post("/api/v1/auth/signup")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupReq))
            );

            // then
            result.andExpect(status().isOk());
            result.andExpect(jsonPath("$.email").value(email));
            result.andExpect(jsonPath("$.nickname").value(nickname));
            then(authService).should().signUp(any(SignUpCommand.class));
        }

        @Test
        @DisplayName("회원가입 시 이메일 중복 예외가 발생하면 409 응답을 반환한다")
        void signUp_fail_when_duplicate_resource_detected_at_database() throws Exception {
            // given
            SignUpRequest signupReq = new SignUpRequest("test@gmail.com", "password1234!@@", "test123");
            given(authService.signUp(signupReq.toCommand()))
                .willThrow(CONFLICTED_EMAIL.defaultException());

            // when & then
            mockMvc.perform(
                    post("/api/v1/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupReq))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICTED_EMAIL"))
                .andExpect(jsonPath("$.message").value(CONFLICTED_EMAIL.defaultMessage()));
        }

        @Test
        @DisplayName("예외 알림 경로에서 추가 예외가 발생해도 원래 500 응답은 유지한다")
        void signUp_fail_when_notification_path_breaks_then_response_is_preserved() throws Exception {
            // given
            SignUpRequest signupReq = new SignUpRequest("test@gmail.com", "password1234!@@", "test123");
            given(userInfoService.getCurrentUserInfo()).willThrow(new IllegalStateException("user-info-failed"));
            given(authService.signUp(signupReq.toCommand()))
                .willThrow(new DataIntegrityViolationException("duplicate key"));

            // when & then
            mockMvc.perform(
                    post("/api/v1/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupReq))
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("예상치 못한 서버 오류가 발생했습니다."));

            then(discordNotificationService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("이메일이 비어 있으면 400 에러 응답을 반환한다")
        void signUp_fail_when_email_is_blank() throws Exception {
            // given
            SignUpRequest signupReq = new SignUpRequest("", "password1234!@@", "test123");

            // when & then
            mockMvc.perform(
                    post("/api/v1/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupReq))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("비밀번호가 비어 있으면 400 에러 응답을 반환한다")
        void signUp_fail_when_password_is_blank() throws Exception {
            SignUpRequest signupReq = new SignUpRequest("test@gmail.com", "", "test123");

            mockMvc.perform(
                    post("/api/v1/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupReq))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("닉네임이 비어 있으면 400 에러 응답을 반환한다")
        void signUp_fail_when_nickname_is_blank() throws Exception {
            SignUpRequest signupReq = new SignUpRequest("test@gmail.com", "password1234!@@", "");

            mockMvc.perform(
                    post("/api/v1/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupReq))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("이메일 형식이 올바르지 않으면 400 에러 응답을 반환한다")
        void signUp_fail_when_email_format_is_invalid() throws Exception {
            SignUpRequest signupReq = new SignUpRequest("invalid-email", "password1234!@@", "test123");

            mockMvc.perform(
                    post("/api/v1/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupReq))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("비밀번호 형식이 올바르지 않으면 400 에러 응답을 반환한다")
        void signUp_fail_when_password_format_is_invalid() throws Exception {
            SignUpRequest signupReq = new SignUpRequest("test@gmail.com", "1234", "test123");

            mockMvc.perform(
                    post("/api/v1/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupReq))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("닉네임 형식이 올바르지 않으면 400 에러 응답을 반환한다")
        void signUp_fail_when_nickname_format_is_invalid() throws Exception {
            SignUpRequest signupReq = new SignUpRequest("test@gmail.com", "password1234!@@", "@@@");

            mockMvc.perform(
                    post("/api/v1/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupReq))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("이메일 길이가 3자 미만이면 400 에러 응답을 반환한다")
        void signUp_fail_when_email_length_is_too_short() throws Exception {
            SignUpRequest signupReq = new SignUpRequest("a@", "password1234!@@", "test123");

            mockMvc.perform(
                    post("/api/v1/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupReq))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("잘못된 JSON 요청이면 400 에러 응답을 반환한다")
        void signUp_fail_when_request_body_is_invalid_json() throws Exception {
            mockMvc.perform(
                    post("/api/v1/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content("{\"invalid-json\"}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_JSON"));
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {
        @Test
        @DisplayName("유효한 로그인 요청이면 토큰 쿠키와 함께 200 응답을 반환한다")
        void login_success() throws Exception {
            // given
            SignInRequest request = new SignInRequest("test@gmail.com", "password1234!@@");
            SignInCommand command = request.toCommand();
            willAnswer(invocation -> {
                HttpServletResponse response = invocation.getArgument(1);
                response.addHeader(HttpHeaders.SET_COOKIE, tokenCookie(ACCESS_TOKEN, "access-token"));
                response.addHeader(HttpHeaders.SET_COOKIE, tokenCookie(REFRESH_TOKEN, "refresh-token"));
                return null;
            })
                .given(authService)
                .login(eq(command), any(HttpServletResponse.class));

            // when
            var result = mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            );

            // then
            result.andExpect(status().isOk());
            result.andExpect(cookie().value(ACCESS_TOKEN, "access-token"));
            result.andExpect(cookie().value(REFRESH_TOKEN, "refresh-token"));
            result.andExpect(cookie().httpOnly(ACCESS_TOKEN, true));
            result.andExpect(cookie().httpOnly(REFRESH_TOKEN, true));
            result.andExpect(cookie().maxAge(ACCESS_TOKEN, 86_400));
            result.andExpect(cookie().maxAge(REFRESH_TOKEN, 86_400));
            result.andExpect(setCookieContains(ACCESS_TOKEN, "Path=/", "SameSite=Strict"));
            result.andExpect(setCookieContains(REFRESH_TOKEN, "Path=/", "SameSite=Strict"));
            then(authService).should().login(eq(command), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("로그인 시 이메일이 비어 있으면 400 에러 응답을 반환한다")
        void login_fail_when_email_is_blank() throws Exception {
            // given
            SignInRequest request = new SignInRequest("", "password1234!@@");

            // when & then
            mockMvc.perform(
                    post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));

            then(authService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("로그인 시 비밀번호가 비어 있으면 400 에러 응답을 반환한다")
        void login_fail_when_password_is_blank() throws Exception {
            SignInRequest request = new SignInRequest("test@gmail.com", "");

            mockMvc.perform(
                    post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));

            then(authService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("로그인 시 이메일 형식이 올바르지 않으면 400 에러 응답을 반환한다")
        void login_fail_when_email_format_is_invalid() throws Exception {
            SignInRequest request = new SignInRequest("invalid-email", "password1234!@@");

            mockMvc.perform(
                    post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));

            then(authService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("로그인 시 비밀번호 형식이 올바르지 않으면 400 에러 응답을 반환한다")
        void login_fail_when_password_format_is_invalid() throws Exception {
            SignInRequest request = new SignInRequest("test@gmail.com", "1234");

            mockMvc.perform(
                    post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));

            then(authService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("로그인 시 잘못된 JSON 요청이면 400 에러 응답을 반환한다")
        void login_fail_when_request_body_is_invalid_json() throws Exception {
            mockMvc.perform(
                    post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"invalid-json\"}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_JSON"));

            then(authService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("로그인 시 인증 정보가 올바르지 않으면 401 에러 응답을 반환한다")
        void login_fail_when_credentials_are_invalid() throws Exception {
            // given
            SignInRequest request = new SignInRequest("test@gmail.com", "password1234!@@");

            willThrow(INVALID_CREDENTIALS.defaultException())
                .given(authService)
                .login(any(SignInCommand.class), any(HttpServletResponse.class));

            // when & then
            mockMvc.perform(
                    post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class TokenRefresh {
        @Test
        @DisplayName("유효한 토큰 재발급 요청이면 새 토큰 쿠키와 함께 200 응답을 반환한다")
        void refresh_success() throws Exception {
            // given
            willAnswer(invocation -> {
                HttpServletResponse response = invocation.getArgument(1);
                response.addHeader(HttpHeaders.SET_COOKIE, tokenCookie(ACCESS_TOKEN, "new-access-token"));
                response.addHeader(HttpHeaders.SET_COOKIE, tokenCookie(REFRESH_TOKEN, "new-refresh-token"));
                return null;
            })
                .given(authService)
                .refresh(any(RefreshCommand.class), any(HttpServletResponse.class));

            mockMvc.perform(
                    post("/api/v1/auth/refresh")
                        .cookie(
                            new Cookie(ACCESS_TOKEN, "expired-access-token"),
                            new Cookie(REFRESH_TOKEN, "valid-refresh-token")
                        )
                )
                .andExpect(status().isOk())
                .andExpect(cookie().value(ACCESS_TOKEN, "new-access-token"))
                .andExpect(cookie().value(REFRESH_TOKEN, "new-refresh-token"))
                .andExpect(cookie().httpOnly(ACCESS_TOKEN, true))
                .andExpect(cookie().httpOnly(REFRESH_TOKEN, true))
                .andExpect(cookie().maxAge(ACCESS_TOKEN, 86_400))
                .andExpect(cookie().maxAge(REFRESH_TOKEN, 86_400))
                .andExpect(setCookieContains(ACCESS_TOKEN, "Path=/", "SameSite=Strict"))
                .andExpect(setCookieContains(REFRESH_TOKEN, "Path=/", "SameSite=Strict"));

            // when & then
            then(authService).should()
                .refresh(eq(new RefreshCommand("expired-access-token", "valid-refresh-token")), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("access token 쿠키가 없으면 401 에러 응답을 반환한다")
        void refresh_fail_when_access_token_is_missing() throws Exception {
            willThrow(ACCESS_TOKEN_INVALID.defaultException())
                .given(authService)
                .refresh(any(RefreshCommand.class), any(HttpServletResponse.class));

            mockMvc.perform(
                    post("/api/v1/auth/refresh")
                        .cookie(new Cookie(REFRESH_TOKEN, "valid-refresh-token"))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ACCESS_TOKEN_INVALID"));

            then(authService).should()
                .refresh(eq(new RefreshCommand(null, "valid-refresh-token")), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("리프레시 토큰이 없으면 400 에러 응답을 반환한다")
        void refresh_fail_when_refresh_token_is_missing() throws Exception {
            // given
            willThrow(REFRESH_TOKEN_MISSING.defaultException())
                .given(authService)
                .refresh(any(RefreshCommand.class), any(HttpServletResponse.class));

            // when & then
            mockMvc.perform(
                    post("/api/v1/auth/refresh")
                        .cookie(new Cookie(ACCESS_TOKEN, "expired-access-token"))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REFRESH_TOKEN_MISSING"));
        }

        @Test
        @DisplayName("리프레시 토큰이 유효하지 않으면 401 에러 응답을 반환한다")
        void refresh_fail_when_refresh_token_is_invalid() throws Exception {
            // given
            willThrow(REFRESH_TOKEN_INVALID.defaultException())
                .given(authService)
                .refresh(any(RefreshCommand.class), any(HttpServletResponse.class));

            // when & then
            mockMvc.perform(
                    post("/api/v1/auth/refresh")
                        .cookie(
                            new Cookie(ACCESS_TOKEN, "expired-access-token"),
                            new Cookie(REFRESH_TOKEN, "invalid-refresh-token")
                        )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("REFRESH_TOKEN_INVALID"));
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {
        @Test
        @DisplayName("로그아웃 요청이면 토큰 쿠키를 비우고 204 응답을 반환한다")
        void logout_success_when_access_token_exists() throws Exception {
            // given
            willAnswer(invocation -> {
                HttpServletResponse response = invocation.getArgument(1);
                response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie(ACCESS_TOKEN));
                response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie(REFRESH_TOKEN));
                return null;
            })
                .given(authService)
                .logout(any(LogoutCommand.class), any(HttpServletResponse.class));

            // when
            mockMvc.perform(
                    post("/api/v1/auth/logout")
                        .cookie(
                            new Cookie(ACCESS_TOKEN, "valid-access-token"),
                            new Cookie(REFRESH_TOKEN, "valid-refresh-token")
                        )
                )
                .andExpect(status().isNoContent())
                .andExpect(cookie().value(ACCESS_TOKEN, ""))
                .andExpect(cookie().value(REFRESH_TOKEN, ""))
                .andExpect(cookie().httpOnly(ACCESS_TOKEN, true))
                .andExpect(cookie().httpOnly(REFRESH_TOKEN, true))
                .andExpect(cookie().maxAge(ACCESS_TOKEN, 0))
                .andExpect(cookie().maxAge(REFRESH_TOKEN, 0))
                .andExpect(setCookieContains(ACCESS_TOKEN, "Path=/", "SameSite=Strict"))
                .andExpect(setCookieContains(REFRESH_TOKEN, "Path=/", "SameSite=Strict"));

            // then
            then(authService).should()
                .logout(eq(new LogoutCommand("valid-access-token", "valid-refresh-token")), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("토큰 쿠키가 없어도 로그아웃 요청이면 204 응답을 반환한다")
        void logout_success_when_access_token_is_missing() throws Exception {
            // given
            willAnswer(invocation -> {
                HttpServletResponse response = invocation.getArgument(1);
                response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie(ACCESS_TOKEN));
                response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie(REFRESH_TOKEN));
                return null;
            })
                .given(authService)
                .logout(any(LogoutCommand.class), any(HttpServletResponse.class));

            // when
            mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().value(ACCESS_TOKEN, ""))
                .andExpect(cookie().value(REFRESH_TOKEN, ""))
                .andExpect(cookie().httpOnly(ACCESS_TOKEN, true))
                .andExpect(cookie().httpOnly(REFRESH_TOKEN, true))
                .andExpect(cookie().maxAge(ACCESS_TOKEN, 0))
                .andExpect(cookie().maxAge(REFRESH_TOKEN, 0))
                .andExpect(setCookieContains(ACCESS_TOKEN, "Path=/", "SameSite=Strict"))
                .andExpect(setCookieContains(REFRESH_TOKEN, "Path=/", "SameSite=Strict"));

            // then
            then(authService).should()
                .logout(eq(new LogoutCommand(null, null)), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("access token 쿠키만 있어도 해당 값이 로그아웃 명령으로 전달된다")
        void logout_success_when_only_access_token_exists() throws Exception {
            // when
            mockMvc.perform(
                    post("/api/v1/auth/logout")
                        .cookie(new Cookie(ACCESS_TOKEN, "valid-access-token"))
                )
                .andExpect(status().isNoContent());

            // then
            then(authService).should()
                .logout(eq(new LogoutCommand("valid-access-token", null)), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("refresh token 쿠키만 있어도 해당 값이 로그아웃 명령으로 전달된다")
        void logout_success_when_only_refresh_token_exists() throws Exception {
            // when
            mockMvc.perform(
                    post("/api/v1/auth/logout")
                        .cookie(new Cookie(REFRESH_TOKEN, "valid-refresh-token"))
                )
                .andExpect(status().isNoContent());

            // then
            then(authService).should()
                .logout(eq(new LogoutCommand(null, "valid-refresh-token")), any(HttpServletResponse.class));
        }
    }

    private static String tokenCookie(String name, String value) {
        return ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(false)
            .path("/")
            .sameSite("Strict")
            .maxAge(86_400)
            .build()
            .toString();
    }

    private static String deleteCookie(String name) {
        return ResponseCookie.from(name, "")
            .httpOnly(true)
            .secure(false)
            .path("/")
            .sameSite("Strict")
            .maxAge(0)
            .build()
            .toString();
    }

    private static ResultMatcher setCookieContains(String cookieName, String... fragments) {
        return result -> {
            String header = result.getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE)
                .stream()
                .filter(value -> value.startsWith(cookieName + "="))
                .findFirst()
                .orElseThrow();

            assertThat(header).contains(fragments);
        };
    }
}
