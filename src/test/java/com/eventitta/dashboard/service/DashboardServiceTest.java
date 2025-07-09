package com.eventitta.dashboard.service;

import com.eventitta.dashboard.dto.response.UserRankingResponse;
import com.eventitta.dashboard.enums.RankingPeriod;
import com.eventitta.dashboard.repository.DashboardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DisplayName("대시보드 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("전체 기간 랭킹 조회 - 기간 제한 없이 전체 순위를 가져온다")
    void givenAllPeriod_whenGetUserRankings_thenReturnsAllTimeRankings() {
        // given
        List<UserRankingResponse> expectedRankings = createMockRankings();
        given(dashboardRepository.findTopRankings(null, 10)).willReturn(expectedRankings);

        // when
        List<UserRankingResponse> result = dashboardService.getUserRankings(RankingPeriod.ALL);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).nickname()).isEqualTo("topUser");
        assertThat(result.get(0).totalPoints()).isEqualTo(1500);
        verify(dashboardRepository).findTopRankings(null, 10);
    }

    @Test
    @DisplayName("주간 랭킹 조회 - 7일 전부터의 순위를 가져온다")
    void givenWeeklyPeriod_whenGetUserRankings_thenReturnsWeeklyRankings() {
        // given
        List<UserRankingResponse> expectedRankings = createMockWeeklyRankings();
        LocalDateTime now = LocalDateTime.now();

        // Mock 시계를 사용하지 않고 ArgumentCaptor로 검증
        given(dashboardRepository.findTopRankings(org.mockito.ArgumentMatchers.any(LocalDateTime.class), eq(10)))
            .willReturn(expectedRankings);

        // when
        List<UserRankingResponse> result = dashboardService.getUserRankings(RankingPeriod.WEEKLY);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).nickname()).isEqualTo("weeklyTop");
        assertThat(result.get(0).totalPoints()).isEqualTo(500);

        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(dashboardRepository).findTopRankings(fromCaptor.capture(), eq(10));

        LocalDateTime capturedFrom = fromCaptor.getValue();
        assertThat(capturedFrom).isBefore(now);
        assertThat(capturedFrom).isAfter(now.minusDays(8)); // 7일 전보다는 이후
        assertThat(capturedFrom).isBefore(now.minusDays(6)); // 6일 전보다는 이전
    }

    @Test
    @DisplayName("빈 랭킹 조회 - 데이터가 없으면 빈 목록을 반환한다")
    void givenNoData_whenGetUserRankings_thenReturnsEmptyList() {
        // given
        given(dashboardRepository.findTopRankings(null, 10)).willReturn(List.of());

        // when
        List<UserRankingResponse> result = dashboardService.getUserRankings(RankingPeriod.ALL);

        // then
        assertThat(result).isEmpty();
        verify(dashboardRepository).findTopRankings(null, 10);
    }

    @Test
    @DisplayName("랭킹 제한 개수 - 항상 10개까지만 조회한다")
    void whenGetUserRankings_thenAlwaysLimitTo10() {
        // given
        given(dashboardRepository.findTopRankings(null, 10)).willReturn(List.of());

        // when
        dashboardService.getUserRankings(RankingPeriod.ALL);

        // then
        verify(dashboardRepository).findTopRankings(null, 10);
    }

    @Test
    @DisplayName("주간 랭킹의 기간 계산 - 정확히 7일 전부터 계산된다")
    void givenWeeklyPeriod_whenGetUserRankings_thenCalculatesExactly7DaysAgo() {
        // given
        given(dashboardRepository.findTopRankings(org.mockito.ArgumentMatchers.any(LocalDateTime.class), eq(10)))
            .willReturn(List.of());

        // when
        dashboardService.getUserRankings(RankingPeriod.WEEKLY);

        // then
        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(dashboardRepository).findTopRankings(fromCaptor.capture(), eq(10));

        LocalDateTime capturedFrom = fromCaptor.getValue();
        LocalDateTime expectedFrom = LocalDateTime.now().minusDays(7);

        long minutesDifference = Math.abs(java.time.temporal.ChronoUnit.MINUTES.between(capturedFrom, expectedFrom));
        assertThat(minutesDifference).isLessThanOrEqualTo(1);
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
