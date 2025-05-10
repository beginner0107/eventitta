package com.eventitta.auth.controller;

import com.eventitta.ControllerTestSupport;
import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import static com.eventitta.common.constants.ValidationMessage.*;
import static com.eventitta.common.exception.CommonErrorCode.INVALID_INPUT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("회원 인증/인가 슬라이스 테스트")
class AuthControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("회원이 유효한 이메일, 비밀번호, 닉네임으로 회원가입하면 사용자 정보가 응답된다.")
    void 회원가입_성공시_사용자_정보가_응답된다() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("user@example.com", "P@ssw0rd!", "johndoe");

        User mockUser = User.builder()
            .id(1L)
            .email("user@example.com")
            .password("encoded")
            .nickname("johndoe")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();

        given(authService.signUp(any())).willReturn(mockUser);

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.nickname").value("johndoe"));
    }

    @Test
    @DisplayName("회원이 잘못된 형식의 이메일로 회원가입하면 이메일 형식 오류 메시지가 응답된다.")
    void 이메일_형식이_유효하지_않으면_에러_메시지를_응답한다() throws Exception {
        SignUpRequest request = new SignUpRequest("invalid-email", "P@ssw0rd!", "johndoe");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("email: " + EMAIL))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @DisplayName("회원이 8자 미만 비밀번호로 회원가입하면 비밀번호 정책 위반 메시지가 응답된다.")
    void 비밀번호_형식이_유효하지_않으면_에러_메시지를_응답한다() throws Exception {
        SignUpRequest request = new SignUpRequest("user@example.com", "1234", "johndoe");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("password: " + PASSWORD))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Password1",    // 특수문자 없음
        "Password!",    // 숫자 없음
        "1234!@#$"      // 영문자 없음
    })
    @DisplayName("비밀번호가 영문자, 숫자, 특수문자를 모두 포함하지 않으면 비밀번호 정책 위반 메시지가 응답된다.")
    void 비밀번호가_형식에_맞지_않으면_에러_메시지를_응답한다(String invalidPassword) throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("user@example.com", invalidPassword, "johndoe");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("password: " + PASSWORD))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @DisplayName("회원이 허용되지 않은 형식의 닉네임으로 회원가입하면 닉네임 정책 위반 메시지가 응답된다.")
    void 닉네임_형식이_유효하지_않으면_에러_메시지를_응답한다() throws Exception {
        SignUpRequest request = new SignUpRequest("user@example.com", "P@ssw0rd!", "!@#");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("nickname: " + NICKNAME))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }
}
