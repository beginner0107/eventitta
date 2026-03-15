package com.eventitta.user.controller;

import com.eventitta.WithMockCustomUser;
import com.eventitta.auth.jwt.service.UserInfoService;
import com.eventitta.common.constants.ValidationMessage;
import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.dto.projection.ActivitySummaryProjection;
import com.eventitta.gamification.service.UserActivityService;
import com.eventitta.notification.resolver.AlertLevelResolver;
import com.eventitta.notification.service.DiscordNotificationService;
import com.eventitta.user.controller.request.ChangePasswordRequest;
import com.eventitta.user.controller.request.UpdateProfileRequest;
import com.eventitta.user.mapper.UserMapperImpl;
import com.eventitta.user.service.UserService;
import com.eventitta.user.service.dto.ChangePasswordCommand;
import com.eventitta.user.service.dto.UpdateProfileCommand;
import com.eventitta.user.service.dto.UserProfileResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserMapperImpl.class)
@DisplayName("사용자 컨트롤러 슬라이스 테스트")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserActivityService userActivityService;

    @MockitoBean
    private DiscordNotificationService discordNotificationService;

    @MockitoBean
    private AlertLevelResolver alertLevelResolver;

    @MockitoBean
    private UserInfoService userInfoService;

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("내 프로필을 조회하면 null 필드를 정규화한 프로필 정보를 반환한다")
    void getMyProfile_returnsNormalizedProfile() throws Exception {
        // given
        UserProfileResult result = new UserProfileResult(
            42L,
            "user@test.com",
            "nick",
            null,
            null,
            null,
            null,
            null,
            null
        );
        given(userService.getProfile(42L)).willReturn(result);

        // when & then
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(42L))
            .andExpect(jsonPath("$.email").value("user@test.com"))
            .andExpect(jsonPath("$.nickname").value("nick"))
            .andExpect(jsonPath("$.profilePictureUrl").value(""))
            .andExpect(jsonPath("$.selfIntro").value(""))
            .andExpect(jsonPath("$.interests.length()").value(0))
            .andExpect(jsonPath("$.address").value(""))
            .andExpect(jsonPath("$.latitude").value(0))
            .andExpect(jsonPath("$.longitude").value(0));
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("존재하지 않는 사용자의 프로필 조회는 404를 반환한다")
    void getMyProfile_userNotFound_returnsNotFound() throws Exception {
        // given
        given(userService.getProfile(42L)).willThrow(com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID.defaultException());

        // when & then
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value(com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID.name()))
            .andExpect(jsonPath("$.message").value(com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID.defaultMessage()));
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("프로필 수정 요청은 service command로 변환되어 204를 반환한다")
    void updateProfile_returnsNoContent() throws Exception {
        // given
        UpdateProfileRequest request = new UpdateProfileRequest(
            "nick",
            "https://example.com/profile.jpg",
            "hello",
            List.of("music", "travel"),
            "Seoul",
            new BigDecimal("37.123456"),
            new BigDecimal("127.123456")
        );

        // when & then
        mockMvc.perform(put("/api/v1/users/me")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        ArgumentCaptor<UpdateProfileCommand> captor = ArgumentCaptor.forClass(UpdateProfileCommand.class);
        verify(userService).updateProfile(eq(42L), captor.capture());
        UpdateProfileCommand command = captor.getValue();
        assertThat(command.nickname()).isEqualTo("nick");
        assertThat(command.profilePictureUrl()).isEqualTo("https://example.com/profile.jpg");
        assertThat(command.selfIntro()).isEqualTo("hello");
        assertThat(command.interests()).containsExactly("music", "travel");
        assertThat(command.address()).isEqualTo("Seoul");
        assertThat(command.latitude()).isEqualByComparingTo("37.123456");
        assertThat(command.longitude()).isEqualByComparingTo("127.123456");
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("닉네임이 비어있으면 프로필 수정 요청이 거부된다")
    void updateProfile_blankNickname_returnsBadRequest() throws Exception {
        // given
        UpdateProfileRequest badRequest = new UpdateProfileRequest(
            " ",
            null,
            null,
            List.of(),
            null,
            null,
            null
        );

        // when & then
        mockMvc.perform(put("/api/v1/users/me")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message").value("nickname: " + ValidationMessage.NICKNAME));
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("이미 사용 중인 닉네임으로 프로필을 수정하면 409를 반환한다")
    void updateProfile_conflictedNickname_returnsConflict() throws Exception {
        // given
        UpdateProfileRequest request = new UpdateProfileRequest(
            "duplicated",
            null,
            null,
            List.of(),
            null,
            null,
            null
        );
        doThrow(com.eventitta.user.exception.UserErrorCode.CONFLICTED_NICKNAME.defaultException())
            .when(userService)
            .updateProfile(eq(42L), org.mockito.ArgumentMatchers.any(UpdateProfileCommand.class));

        // when & then
        mockMvc.perform(put("/api/v1/users/me")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value(com.eventitta.user.exception.UserErrorCode.CONFLICTED_NICKNAME.name()))
            .andExpect(jsonPath("$.message").value(com.eventitta.user.exception.UserErrorCode.CONFLICTED_NICKNAME.defaultMessage()));
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("회원 탈퇴 요청은 204를 반환한다")
    void deleteMe_returnsNoContent() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/users/me"))
            .andExpect(status().isNoContent());

        verify(userService).deleteUser(42L);
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("존재하지 않는 사용자의 회원 탈퇴는 404를 반환한다")
    void deleteMe_userNotFound_returnsNotFound() throws Exception {
        // given
        doThrow(com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID.defaultException())
            .when(userService)
            .deleteUser(42L);

        // when & then
        mockMvc.perform(delete("/api/v1/users/me"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value(com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID.name()))
            .andExpect(jsonPath("$.message").value(com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID.defaultMessage()));
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("비밀번호 변경 요청은 service command로 변환되어 204를 반환한다")
    void changePassword_returnsNoContent() throws Exception {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest("oldPw123!", "NewPw123!");

        // when & then
        mockMvc.perform(put("/api/v1/users/me/password")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        ArgumentCaptor<ChangePasswordCommand> captor = ArgumentCaptor.forClass(ChangePasswordCommand.class);
        verify(userService).changePassword(eq(42L), captor.capture());
        ChangePasswordCommand command = captor.getValue();
        assertThat(command.currentPassword()).isEqualTo("oldPw123!");
        assertThat(command.newPassword()).isEqualTo("NewPw123!");
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("새 비밀번호 형식이 올바르지 않으면 400을 반환한다")
    void changePassword_invalidNewPassword_returnsBadRequest() throws Exception {
        // given
        ChangePasswordRequest badRequest = new ChangePasswordRequest("oldPw123!", "bad");

        // when & then
        mockMvc.perform(put("/api/v1/users/me/password")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message").value("newPassword: " + ValidationMessage.PASSWORD));
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("현재 비밀번호가 틀리면 400을 반환한다")
    void changePassword_wrongCurrentPassword_returnsBadRequest() throws Exception {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest("oldPw123!", "NewPw123!");
        doThrow(com.eventitta.user.exception.UserErrorCode.INVALID_CURRENT_PASSWORD.defaultException())
            .when(userService)
            .changePassword(eq(42L), org.mockito.ArgumentMatchers.any(ChangePasswordCommand.class));

        // when & then
        mockMvc.perform(put("/api/v1/users/me/password")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(com.eventitta.user.exception.UserErrorCode.INVALID_CURRENT_PASSWORD.name()))
            .andExpect(jsonPath("$.message").value(com.eventitta.user.exception.UserErrorCode.INVALID_CURRENT_PASSWORD.defaultMessage()));
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("내 활동 요약 조회는 projection을 응답 DTO로 변환해 반환한다")
    void getMyActivitySummary_returnsMappedResponses() throws Exception {
        // given
        given(userActivityService.getActivitySummaryProjection(42L)).willReturn(List.of(
            activitySummaryProjection(ActivityType.USER_LOGIN, 2L, 10L),
            activitySummaryProjection(ActivityType.CREATE_POST, 1L, 10L)
        ));

        // when & then
        mockMvc.perform(get("/api/v1/users/me/activities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].activityType").value(ActivityType.USER_LOGIN.name()))
            .andExpect(jsonPath("$[0].count").value(2))
            .andExpect(jsonPath("$[0].totalPoints").value(10))
            .andExpect(jsonPath("$[1].activityType").value(ActivityType.CREATE_POST.name()))
            .andExpect(jsonPath("$[1].count").value(1))
            .andExpect(jsonPath("$[1].totalPoints").value(10));
    }

    private ActivitySummaryProjection activitySummaryProjection(ActivityType activityType, long count, long totalPoints) {
        return new ActivitySummaryProjection() {
            @Override
            public ActivityType getActivityType() {
                return activityType;
            }

            @Override
            public long getCount() {
                return count;
            }

            @Override
            public long getTotalPoints() {
                return totalPoints;
            }
        };
    }
}
