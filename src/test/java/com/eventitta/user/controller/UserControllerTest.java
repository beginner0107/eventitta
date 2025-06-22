package com.eventitta.user.controller;

import com.eventitta.ControllerTestSupport;
import com.eventitta.WithMockCustomUser;
import com.eventitta.common.constants.ValidationMessage;
import com.eventitta.user.dto.ChangePasswordRequest;
import com.eventitta.user.dto.UpdateProfileRequest;
import com.eventitta.user.dto.UserProfileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("사용자 컨트롤러 슬라이스 테스트")
class UserControllerTest extends ControllerTestSupport {

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("내 프로필을 조회하면 정보가 반환된다")
    void getMyProfile_returnsProfile() throws Exception {
        UserProfileResponse resp = new UserProfileResponse(
            42L,
            "user@test.com",
            "nick",
            "",
            "",
            List.of("a"),
            "addr",
            BigDecimal.ONE,
            BigDecimal.TEN
        );
        given(userService.getProfile(42L)).willReturn(resp);

        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(42L))
            .andExpect(jsonPath("$.email").value(resp.email()));
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("프로필 수정시 성공하면 204가 반환된다")
    void updateProfile_returnsNoContent() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest(
            "nick",
            null,
            null,
            List.of(),
            null,
            null,
            null
        );
        doNothing().when(userService).updateProfile(eq(42L), any(UpdateProfileRequest.class));

        mockMvc.perform(put("/api/v1/users/me")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("닉네임이 비어있으면 수정 요청이 거부된다")
    void updateProfile_blankNickname_returnsBadRequest() throws Exception {
        UpdateProfileRequest bad = new UpdateProfileRequest(
            " ",
            null,
            null,
            List.of(),
            null,
            null,
            null
        );

        mockMvc.perform(put("/api/v1/users/me")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("nickname: " + ValidationMessage.NICKNAME));
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("회원 탈퇴시 204가 반환된다")
    void deleteMe_returnsNoContent() throws Exception {
        doNothing().when(userService).deleteUser(42L);

        mockMvc.perform(delete("/api/v1/users/me"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("비밀번호 변경 성공 시 204가 반환된다")
    void changePassword_returnsNoContent() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest("oldPw123!", "NewPw123!");
        doNothing().when(userService).changePassword(eq(42L), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/v1/users/me/password")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("새 비밀번호 형식이 올바르지 않으면 400을 반환한다")
    void changePassword_invalidNewPassword_returnsBadRequest() throws Exception {
        ChangePasswordRequest bad = new ChangePasswordRequest("oldPw123!", "bad");

        mockMvc.perform(put("/api/v1/users/me/password")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("newPassword: " + ValidationMessage.PASSWORD));
    }
}
