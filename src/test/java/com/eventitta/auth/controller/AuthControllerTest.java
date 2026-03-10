package com.eventitta.auth.controller;

import com.eventitta.auth.controller.request.SignUpRequest;
import com.eventitta.auth.dto.request.SignInRequest;
import com.eventitta.auth.jwt.service.UserInfoService;
import com.eventitta.auth.service.AuthService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.eventitta.auth.constants.AuthConstants.ACCESS_TOKEN;
import static com.eventitta.auth.constants.AuthConstants.REFRESH_TOKEN;
import static com.eventitta.auth.exception.AuthErrorCode.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        @DisplayName("유효한 회원가입 요청이면 200과 회원 정보를 반환한다")
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
        @DisplayName("유효한 로그인 요청이면 200 응답을 반환한다")
        void login_success() throws Exception {
            // given
            SignInRequest request = new SignInRequest("test@gmail.com", "password1234!@@");

            // when
            var result = mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            );

            // then
            result.andExpect(status().isOk());
            then(authService).should().login(any(SignInRequest.class), any(HttpServletResponse.class));
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
                .login(any(SignInRequest.class), any(HttpServletResponse.class));

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
    @DisplayName("토큰재발급")
    class TokenRefresh {
        @Test
        @DisplayName("유효한 토큰 재발급 요청이면 200 응답을 반환한다")
        void refresh_success() throws Exception {
            // given
            mockMvc.perform(
                    post("/api/v1/auth/refresh")
                        .cookie(
                            new Cookie(ACCESS_TOKEN, "expired-access-token"),
                            new Cookie(REFRESH_TOKEN, "valid-refresh-token")
                        )
                )
                .andExpect(status().isOk());

            // when & then
            then(authService).should()
                .refresh(eq("expired-access-token"), eq("valid-refresh-token"), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("리프레시 토큰이 없으면 400 에러 응답을 반환한다")
        void refresh_fail_when_refresh_token_is_missing() throws Exception {
            // given
            willThrow(REFRESH_TOKEN_MISSING.defaultException())
                .given(authService)
                .refresh(any(), any(), any(HttpServletResponse.class));

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
                .refresh(any(), any(), any(HttpServletResponse.class));

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
}
