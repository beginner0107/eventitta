package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.RankingType;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingSyncServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserActivityRepository userActivityRepository;

    @Mock
    private RankingService rankingService;

    @InjectMocks
    private RankingSyncService rankingSyncService;

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        testUser1 = createUser(1L, "user1", 100);
        testUser2 = createUser(2L, "user2", 200);
        testUser3 = createUser(3L, "user3", 0); // 0점 유저
    }

    @Test
    @DisplayName("전체 동기화가 성공적으로 수행된다")
    void syncAllRankingsFromDatabase_Success() {
        // given
        when(userRepository.count()).thenReturn(3L);

        Page<User> userPage = new PageImpl<>(List.of(testUser1, testUser2, testUser3));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        when(userActivityRepository.countByUserId(1L)).thenReturn(5L);
        when(userActivityRepository.countByUserId(2L)).thenReturn(10L);
        when(userActivityRepository.countByUserId(3L)).thenReturn(0L);

        // when
        rankingSyncService.syncAllRankingsFromDatabase();

        // then
        // 포인트 랭킹 업데이트 확인 (0점 유저는 제외)
        ArgumentCaptor<Map<Long, Double>> pointsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(rankingService).updateScoresBatch(eq(RankingType.POINTS), pointsCaptor.capture());
        Map<Long, Double> capturedPoints = pointsCaptor.getValue();
        assertThat(capturedPoints).hasSize(2);
        assertThat(capturedPoints.get(1L)).isEqualTo(100.0);
        assertThat(capturedPoints.get(2L)).isEqualTo(200.0);

        // 활동량 랭킹 업데이트 확인 (활동이 없는 유저는 제외)
        ArgumentCaptor<Map<Long, Double>> activityCaptor = ArgumentCaptor.forClass(Map.class);
        verify(rankingService).updateScoresBatch(eq(RankingType.ACTIVITY_COUNT), activityCaptor.capture());
        Map<Long, Double> capturedActivities = activityCaptor.getValue();
        assertThat(capturedActivities).hasSize(2);
        assertThat(capturedActivities.get(1L)).isEqualTo(5.0);
        assertThat(capturedActivities.get(2L)).isEqualTo(10.0);
    }

    @Test
    @DisplayName("전체 동기화 중 예외 발생 시 RuntimeException으로 전파된다")
    void syncAllRankingsFromDatabase_ExceptionPropagation() {
        // given
        when(userRepository.count()).thenThrow(new RuntimeException("Database error"));

        // when & then
        assertThatThrownBy(() -> rankingSyncService.syncAllRankingsFromDatabase())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to sync rankings");
    }

    @Test
    @DisplayName("포인트 순위 페이징 동기화가 정상 동작한다")
    void syncPointsRanking_WithPaging() {
        // given
        when(userRepository.count()).thenReturn(100L);

        // SYNC_BATCH_SIZE = 1000이므로, 100개는 한 페이지로 처리됨
        Page<User> page = new PageImpl<>(List.of(testUser1, testUser2, testUser3));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        // 활동량 카운트 모킹
        when(userActivityRepository.countByUserId(1L)).thenReturn(5L);
        when(userActivityRepository.countByUserId(2L)).thenReturn(10L);
        when(userActivityRepository.countByUserId(3L)).thenReturn(0L);

        // when
        rankingSyncService.syncAllRankingsFromDatabase();

        // then
        verify(userRepository, times(2)).findAll(any(Pageable.class)); // 포인트와 활동량 각 1번씩
        // 0점 유저는 제외되므로 포인트 업데이트는 1번만
        verify(rankingService, times(1)).updateScoresBatch(eq(RankingType.POINTS), any());
        verify(rankingService, times(1)).updateScoresBatch(eq(RankingType.ACTIVITY_COUNT), any());
    }

    @Test
    @DisplayName("특정 유저 동기화가 정상적으로 수행된다")
    void syncUserRanking_Success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userActivityRepository.countByUserId(1L)).thenReturn(5L);

        // when
        rankingSyncService.syncUserRanking(1L);

        // then
        verify(rankingService).updatePointsRanking(1L, 100);
        verify(rankingService).updateActivityCountRanking(1L, 5);
    }

    @Test
    @DisplayName("0점 유저는 포인트 순위에서 제거된다")
    void syncUserRanking_ZeroPoints_RemoveFromRanking() {
        // given
        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser3));
        when(userActivityRepository.countByUserId(3L)).thenReturn(0L);

        // when
        rankingSyncService.syncUserRanking(3L);

        // then
        verify(rankingService).removeUser(RankingType.POINTS, 3L);
        verify(rankingService).removeUser(RankingType.ACTIVITY_COUNT, 3L);
    }

    @Test
    @DisplayName("존재하지 않는 유저 동기화 시 경고 로그만 출력한다")
    void syncUserRanking_UserNotFound() {
        // given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when
        rankingSyncService.syncUserRanking(999L);

        // then
        verify(rankingService, never()).updatePointsRanking(anyLong(), anyInt());
        verify(rankingService, never()).updateActivityCountRanking(anyLong(), anyLong());
    }

    @Test
    @DisplayName("유저 동기화 중 예외 발생 시 로그만 출력하고 중단하지 않는다")
    void syncUserRanking_ExceptionHandling() {
        // given
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        // when (예외를 던지지 않아야 함)
        rankingSyncService.syncUserRanking(1L);

        // then
        verify(rankingService, never()).updatePointsRanking(anyLong(), anyInt());
    }

    @Test
    @DisplayName("증분 동기화가 최근 활동 유저만 처리한다")
    void syncRecentlyActiveUsers_Success() {
        // given
        List<Long> activeUserIds = Arrays.asList(1L, 2L);
        when(userActivityRepository.findRecentlyActiveUserIds(24)).thenReturn(activeUserIds);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser2));

        when(userActivityRepository.countByUserId(1L)).thenReturn(5L);
        when(userActivityRepository.countByUserId(2L)).thenReturn(10L);

        // when
        rankingSyncService.syncRecentlyActiveUsers();

        // then
        ArgumentCaptor<Map<Long, Double>> pointsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(rankingService).updateScoresBatch(eq(RankingType.POINTS), pointsCaptor.capture());
        Map<Long, Double> capturedPoints = pointsCaptor.getValue();
        assertThat(capturedPoints).hasSize(2);
        assertThat(capturedPoints.get(1L)).isEqualTo(100.0);
        assertThat(capturedPoints.get(2L)).isEqualTo(200.0);

        ArgumentCaptor<Map<Long, Double>> activityCaptor = ArgumentCaptor.forClass(Map.class);
        verify(rankingService).updateScoresBatch(eq(RankingType.ACTIVITY_COUNT), activityCaptor.capture());
        Map<Long, Double> capturedActivities = activityCaptor.getValue();
        assertThat(capturedActivities).hasSize(2);
        assertThat(capturedActivities.get(1L)).isEqualTo(5.0);
        assertThat(capturedActivities.get(2L)).isEqualTo(10.0);
    }

    @Test
    @DisplayName("최근 활동 유저가 없으면 동기화를 건너뛴다")
    void syncRecentlyActiveUsers_NoActiveUsers() {
        // given
        when(userActivityRepository.findRecentlyActiveUserIds(24)).thenReturn(Collections.emptyList());

        // when
        rankingSyncService.syncRecentlyActiveUsers();

        // then
        verify(userRepository, never()).findById(anyLong());
        verify(rankingService, never()).updateScoresBatch(any(), any());
    }

    @Test
    @DisplayName("증분 동기화 중 일부 유저 조회 실패 시 계속 진행된다")
    void syncRecentlyActiveUsers_PartialFailure() {
        // given
        List<Long> activeUserIds = Arrays.asList(1L, 999L, 2L); // 999L은 존재하지 않는 유저
        when(userActivityRepository.findRecentlyActiveUserIds(24)).thenReturn(activeUserIds);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser2));

        when(userActivityRepository.countByUserId(1L)).thenReturn(5L);
        when(userActivityRepository.countByUserId(999L)).thenReturn(0L);  // 존재하지 않는 유저
        when(userActivityRepository.countByUserId(2L)).thenReturn(10L);

        // when
        rankingSyncService.syncRecentlyActiveUsers();

        // then
        ArgumentCaptor<Map<Long, Double>> pointsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(rankingService).updateScoresBatch(eq(RankingType.POINTS), pointsCaptor.capture());
        Map<Long, Double> capturedPoints = pointsCaptor.getValue();
        assertThat(capturedPoints).hasSize(2); // 999L은 제외됨
        assertThat(capturedPoints).containsKeys(1L, 2L);
    }

    @Test
    @DisplayName("증분 동기화 중 예외 발생 시 로그만 출력하고 중단하지 않는다")
    void syncRecentlyActiveUsers_ExceptionHandling() {
        // given
        when(userActivityRepository.findRecentlyActiveUserIds(24))
                .thenThrow(new RuntimeException("Database error"));

        // when (예외를 던지지 않아야 함)
        rankingSyncService.syncRecentlyActiveUsers();

        // then
        verify(rankingService, never()).updateScoresBatch(any(), any());
    }

    private User createUser(Long id, String nickname, int points) {
        User user = User.builder()
                .email(nickname + "@test.com")
                .nickname(nickname)
                .password("password")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "points", points);
        return user;
    }
}
