package com.eventitta.auth.controller;

import com.eventitta.auth.dto.request.SignUpRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.eventitta.common.constants.ValidationMessage.*;
import static com.eventitta.common.exception.CommonErrorCode.INVALID_INPUT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("회원 통합 테스트")
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("회원이 이메일, 비밀번호, 닉네임으로 회원가입하면 사용자 정보가 응답된다.")
    void 회원가입_정상_요청_시_사용자_정보가_응답된다() throws Exception {
        // given
        var request = new SignUpRequest("test@example.com", "P@ssw0rd!", "tester");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.nickname").value("tester"));
    }

    @Test
    @DisplayName("회원이 유효하지 않은 이메일로 회원가입하면 이메일 형식 오류 메시지가 반환된다.")
    void 이메일_형식이_유효하지_않으면_에러를_응답한다() throws Exception {
        var request = new SignUpRequest("invalid-email", "P@ssw0rd!", "tester");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("email: " + EMAIL))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @DisplayName("회원이 형식에 맞지 않는 비밀번호로 회원가입하면 비밀번호 정책 위반 메시지가 반환된다.")
    void 비밀번호_형식이_유효하지_않으면_에러를_응답한다() throws Exception {
        var request = new SignUpRequest("user@example.com", "1234", "tester");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("password: " + PASSWORD))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @DisplayName("회원이 허용되지 않은 형식의 닉네임으로 회원가입하면 닉네임 정책 위반 메시지가 반환된다.")
    void 닉네임_형식이_유효하지_않으면_에러를_응답한다() throws Exception {
        var request = new SignUpRequest("user@example.com", "P@ssw0rd!", "!@#");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("nickname: " + NICKNAME))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

}
