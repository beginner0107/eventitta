package com.eventitta.meeting.controller;

import com.eventitta.WithMockCustomUser;
import com.eventitta.auth.config.SecurityConfig;
import com.eventitta.auth.jwt.service.UserInfoService;
import com.eventitta.notification.resolver.AlertLevelResolver;
import com.eventitta.notification.service.DiscordNotificationService;
import com.eventitta.meeting.domain.MeetingStatus;
import com.eventitta.meeting.domain.ParticipantStatus;
import com.eventitta.meeting.dto.request.MeetingCreateRequest;
import com.eventitta.meeting.dto.request.MeetingUpdateRequest;
import com.eventitta.meeting.dto.response.ParticipantResponse;
import com.eventitta.meeting.service.MeetingService;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.jwt.service.CustomUserDetailsService;
import com.eventitta.auth.jwt.JwtAuthenticationEntryPoint;
import com.eventitta.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MeetingController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
@DisplayName("모임 컨트롤러 슬라이스 테스트")
class MeetingControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    MeetingService meetingService;
    @MockitoBean
    JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean
    protected DiscordNotificationService discordNotificationService;
    @MockitoBean
    protected AlertLevelResolver alertLevelResolver;
    @MockitoBean
    protected UserRepository userRepository;
    @MockitoBean
    protected UserInfoService userInfoService;

    @Test
    @WithMockCustomUser
    @DisplayName("모임을 생성하면 201을 반환한다")
    void createMeeting_returnsCreated() throws Exception {
        MeetingCreateRequest req = new MeetingCreateRequest(
            "title", null,
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(2),
            10, null, null, null
        );
        given(meetingService.createMeeting(eq(42L), any(MeetingCreateRequest.class))).willReturn(1L);

        mockMvc.perform(post("/api/v1/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/meetings/1"));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("모임을 수정하면 200을 반환한다")
    void updateMeeting_returnsOk() throws Exception {
        doNothing().when(meetingService).updateMeeting(eq(42L), eq(1L), any(MeetingUpdateRequest.class));
        MeetingUpdateRequest req = new MeetingUpdateRequest(
            "new", null,
            LocalDateTime.now().plusDays(2),
            LocalDateTime.now().plusDays(3),
            5, null, null, null,
            MeetingStatus.RECRUITING
        );
        mockMvc.perform(put("/api/v1/meetings/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockCustomUser
    @DisplayName("참가 신청 승인 시 200을 반환한다")
    void approveParticipant_returnsOk() throws Exception {
        ParticipantResponse resp = new ParticipantResponse(
            10L, 42L, "스프링러버", "https://example.com/profile.jpg",
            ParticipantStatus.APPROVED);
        given(meetingService.approveParticipant(42L, 1L, 10L)).willReturn(resp);

        mockMvc.perform(put("/api/v1/meetings/1/participants/10/approve"))
            .andExpect(status().isOk());
    }
}
