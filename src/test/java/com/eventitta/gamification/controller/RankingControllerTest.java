package com.eventitta.gamification.controller;

import com.eventitta.WithMockCustomUser;
import com.eventitta.auth.jwt.JwtAuthenticationEntryPoint;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.jwt.filter.JwtAuthenticationFilter;
import com.eventitta.auth.domain.UserPrincipal;
import com.eventitta.auth.jwt.service.UserInfoService;
import com.eventitta.common.exception.GlobalExceptionHandler;
import com.eventitta.gamification.domain.RankingType;
import com.eventitta.gamification.dto.response.RankingPageResponse;
import com.eventitta.gamification.dto.response.UserRankResponse;
import com.eventitta.gamification.exception.RankingException;
import com.eventitta.gamification.service.RankingService;
import com.eventitta.notification.service.SlackNotificationService;
import com.eventitta.notification.resolver.AlertLevelResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.eventitta.gamification.exception.RankingErrorCode.RANKING_NOT_FOUND;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(
        controllers = RankingController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, RankingControllerTestConfig.class})
class RankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RankingService rankingService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private SlackNotificationService slackNotificationService;

    @MockitoBean
    private AlertLevelResolver alertLevelResolver;

    @MockitoBean
    private UserInfoService userInfoService;

    @Test
    @DisplayName("Top N 순위 조회 - 성공")
    void getTopRankings_Success() throws Exception {
        // given
        List<UserRankResponse> rankings = List.of(
                new UserRankResponse(1L, "user1", "avatarUrl1", 1000, 1),
                new UserRankResponse(2L, "user2", "avatarUrl2", 900, 2),
                new UserRankResponse(3L, "user3", "avatarUrl3", 800, 3)
        );
        RankingPageResponse response = new RankingPageResponse(
                rankings,
                100L,
                RankingType.POINTS
        );

        given(rankingService.getTopRankings(eq(RankingType.POINTS), eq(100)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/rankings/top")
                        .param("type", "POINTS")
                        .param("limit", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("POINTS"))
                .andExpect(jsonPath("$.rankings", hasSize(3)))
                .andExpect(jsonPath("$.rankings[0].userId").value(1))
                .andExpect(jsonPath("$.rankings[0].score").value(1000.0))
                .andExpect(jsonPath("$.rankings[0].rank").value(1))
                .andExpect(jsonPath("$.totalUsers").value(100));

        verify(rankingService, times(1)).getTopRankings(RankingType.POINTS, 100);
    }

    @Test
    @DisplayName("Top N 순위 조회 - 기본값 사용")
    void getTopRankings_WithDefaults() throws Exception {
        // given
        RankingPageResponse response = new RankingPageResponse(
                List.of(),
                0L,
                RankingType.POINTS
        );

        given(rankingService.getTopRankings(eq(RankingType.POINTS), eq(100)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/rankings/top")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("POINTS"))
                .andExpect(jsonPath("$.rankings", hasSize(0)));

        verify(rankingService, times(1)).getTopRankings(RankingType.POINTS, 100);
    }

    @Test
    @DisplayName("Top N 순위 조회 - 활동량 타입")
    void getTopRankings_ActivityType() throws Exception {
        // given
        RankingPageResponse response = new RankingPageResponse(
                List.of(
                        new UserRankResponse(1L, "user1", null, 50, 1)
                ),
                1L,
                RankingType.ACTIVITY_COUNT
        );

        given(rankingService.getTopRankings(eq(RankingType.ACTIVITY_COUNT), eq(50)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/rankings/top")
                        .param("type", "ACTIVITY_COUNT")
                        .param("limit", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ACTIVITY_COUNT"));
    }

    @Test
    @DisplayName("Top N 순위 조회 - 유효성 검사 실패 (limit 초과)")
    void getTopRankings_ValidationFailure() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/rankings/top")
                        .param("limit", "501") // 최대값 500 초과
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rankingService, never()).getTopRankings(any(), anyInt());
    }

    @Test
    @DisplayName("내 순위 조회 - 성공")
    void getMyRank_Success() throws Exception {
        // given
        Long userId = 1L;
        UserRankResponse response = new UserRankResponse(
                userId,
                "testUser",
                "avatarUrl",
                1000,
                5
        );

        given(rankingService.getUserRank(eq(RankingType.POINTS), eq(userId)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/rankings/me")
                        .param("type", "POINTS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setUserPrincipal(() -> "1");
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.nickname").value("testUser"))
                .andExpect(jsonPath("$.score").value(1000.0))
                .andExpect(jsonPath("$.rank").value(5));

        verify(rankingService, times(1)).getUserRank(RankingType.POINTS, userId);
    }

    @Test
    @DisplayName("내 순위 조회 - 인증되지 않은 요청")
    void getMyRank_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/rankings/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(rankingService, never()).getUserRank(any(), anyLong());
    }

    @Test
    @DisplayName("특정 유저 순위 조회 - 성공")
    void getUserRank_Success() throws Exception {
        // given
        Long userId = 2L;
        UserRankResponse response = new UserRankResponse(
                userId,
                "otherUser",
                null,
                800,
                10
        );

        given(rankingService.getUserRank(eq(RankingType.POINTS), eq(userId)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/rankings/users/{userId}", userId)
                        .param("type", "POINTS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.nickname").value("otherUser"))
                .andExpect(jsonPath("$.score").value(800.0))
                .andExpect(jsonPath("$.rank").value(10));

        verify(rankingService, times(1)).getUserRank(RankingType.POINTS, userId);
    }

    @Test
    @DisplayName("특정 유저 순위 조회 - 존재하지 않는 유저")
    void getUserRank_UserNotFound() throws Exception {
        // given
        Long userId = 999L;
        given(rankingService.getUserRank(eq(RankingType.POINTS), eq(userId)))
                .willThrow(new RankingException(RANKING_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/rankings/users/{userId}", userId)
                        .param("type", "POINTS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(rankingService, times(1)).getUserRank(RankingType.POINTS, userId);
    }

    @Test
    @DisplayName("순위 통계 조회 - 성공")
    void getRankingStats_Success() throws Exception {
        // given
        given(rankingService.getTotalUsers(RankingType.POINTS)).willReturn(100L);
        given(rankingService.getTotalUsers(RankingType.ACTIVITY_COUNT)).willReturn(80L);

        // when & then
        mockMvc.perform(get("/api/v1/rankings/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsRankingCount").value(100))
                .andExpect(jsonPath("$.activityRankingCount").value(80));

        verify(rankingService, times(1)).getTotalUsers(RankingType.POINTS);
        verify(rankingService, times(1)).getTotalUsers(RankingType.ACTIVITY_COUNT);
    }

    @Test
    @DisplayName("순위 통계 조회 - null 값 처리")
    void getRankingStats_WithNullValues() throws Exception {
        // given
        given(rankingService.getTotalUsers(RankingType.POINTS)).willReturn(null);
        given(rankingService.getTotalUsers(RankingType.ACTIVITY_COUNT)).willReturn(50L);

        // when & then
        mockMvc.perform(get("/api/v1/rankings/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsRankingCount").value(0))
                .andExpect(jsonPath("$.activityRankingCount").value(50));
    }
}
