package com.eventitta.auth.controller;

import com.eventitta.auth.controller.request.SignUpRequest;
import com.eventitta.auth.jwt.service.UserInfoService;
import com.eventitta.auth.service.AuthService;
import com.eventitta.auth.service.dto.SignUpCommand;
import com.eventitta.auth.service.dto.SignUpResult;
import com.eventitta.notification.resolver.AlertLevelResolver;
import com.eventitta.notification.service.DiscordNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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
}
