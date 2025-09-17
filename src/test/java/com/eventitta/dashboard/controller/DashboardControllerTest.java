package com.eventitta.dashboard.controller;

import com.eventitta.ControllerTestSupport;
import com.eventitta.dashboard.dto.response.UserRankingResponse;
import com.eventitta.dashboard.enums.RankingPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("대시보드 컨트롤러 테스트")
class DashboardControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("랭킹 조회 - 기본 요청시 전체 기간 순위가 나온다")
    void givenNoParameter_whenGetRankings_thenReturnsAllPeriodRankings() throws Exception {
        // given
        List<UserRankingResponse> mockRankings = createMockRankings();
        given(dashboardService.getUserRankings(RankingPeriod.ALL)).willReturn(mockRankings);

        // when & then
        mockMvc.perform(get("/api/v1/dashboard/rankings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].nickname").value("topUser"))
            .andExpect(jsonPath("$[0].totalPoints").value(1500))
            .andExpect(jsonPath("$[1].nickname").value("secondUser"))
            .andExpect(jsonPath("$[1].totalPoints").value(1200))
            .andExpect(jsonPath("$[2].nickname").value("thirdUser"))
            .andExpect(jsonPath("$[2].totalPoints").value(800));
    }

    @Test
    @DisplayName("랭킹 조회 - 전체 기간으로 요청하면 전체 기간 순위가 나온다")
    void givenAllPeriod_whenGetRankings_thenReturnsAllPeriodRankings() throws Exception {
        // given
        List<UserRankingResponse> mockRankings = createMockRankings();
        given(dashboardService.getUserRankings(RankingPeriod.ALL)).willReturn(mockRankings);

        // when & then
        mockMvc.perform(get("/api/v1/dashboard/rankings?period=all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].nickname").value("topUser"))
            .andExpect(jsonPath("$[0].totalPoints").value(1500));
    }

    @Test
    @DisplayName("랭킹 조회 - 주간으로 요청하면 주간 순위가 나온다")
    void givenWeeklyPeriod_whenGetRankings_thenReturnsWeeklyRankings() throws Exception {
        // given
        List<UserRankingResponse> mockWeeklyRankings = createMockWeeklyRankings();
        given(dashboardService.getUserRankings(RankingPeriod.WEEKLY)).willReturn(mockWeeklyRankings);

        // when & then
        mockMvc.perform(get("/api/v1/dashboard/rankings?period=weekly"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].nickname").value("weeklyTop"))
            .andExpect(jsonPath("$[0].totalPoints").value(500))
            .andExpect(jsonPath("$[1].nickname").value("weeklySecond"))
            .andExpect(jsonPath("$[1].totalPoints").value(300));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "monthly", "daily", "ALL"})
    @DisplayName("랭킹 조회 - 잘못된 기간을 입력해도 전체 기간 순위가 나온다")
    void givenInvalidPeriod_whenGetRankings_thenReturnsAllPeriodRankings(String invalidPeriod) throws Exception {
        // given
        List<UserRankingResponse> mockRankings = createMockRankings();
        given(dashboardService.getUserRankings(RankingPeriod.ALL)).willReturn(mockRankings);

        // when & then
        mockMvc.perform(get("/api/v1/dashboard/rankings?period=" + invalidPeriod))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("랭킹 조회 - 대문자 WEEKLY도 주간으로 처리된다")
    void givenUppercaseWeekly_whenGetRankings_thenReturnsWeeklyRankings() throws Exception {
        // given
        List<UserRankingResponse> mockWeeklyRankings = createMockWeeklyRankings();
        given(dashboardService.getUserRankings(RankingPeriod.WEEKLY)).willReturn(mockWeeklyRankings);

        // when & then
        mockMvc.perform(get("/api/v1/dashboard/rankings?period=WEEKLY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("랭킹 조회 - 순위 데이터가 없으면 빈 목록이 나온다")
    void givenNoRankingData_whenGetRankings_thenReturnsEmptyArray() throws Exception {
        // given
        given(dashboardService.getUserRankings(any(RankingPeriod.class))).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/dashboard/rankings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("랭킹 조회 - 대소문자 구분 없이 기간을 입력할 수 있다")
    void givenMixedCasePeriod_whenGetRankings_thenHandlesCaseInsensitively() throws Exception {
        // given
        List<UserRankingResponse> mockWeeklyRankings = createMockWeeklyRankings();
        given(dashboardService.getUserRankings(RankingPeriod.WEEKLY)).willReturn(mockWeeklyRankings);

        // when & then
        mockMvc.perform(get("/api/v1/dashboard/rankings?period=WeEkLy"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }

    private List<UserRankingResponse> createMockRankings() {
        return Arrays.asList(
            new UserRankingResponse("topUser", 1500, LocalDateTime.now().minusDays(1)),
            new UserRankingResponse("secondUser", 1200, LocalDateTime.now().minusDays(2)),
            new UserRankingResponse("thirdUser", 800, LocalDateTime.now().minusDays(3))
        );
    }

    private List<UserRankingResponse> createMockWeeklyRankings() {
        return Arrays.asList(
            new UserRankingResponse("weeklyTop", 500, LocalDateTime.now().minusHours(1)),
            new UserRankingResponse("weeklySecond", 300, LocalDateTime.now().minusHours(2))
        );
    }
}
