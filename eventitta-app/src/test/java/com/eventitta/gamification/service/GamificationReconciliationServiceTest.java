package com.eventitta.domain.gamification.service;

import com.eventitta.domain.gamification.domain.RankingType;
import com.eventitta.domain.gamification.domain.RewardActionType;
import com.eventitta.domain.gamification.domain.UserActivityStats;
import com.eventitta.domain.gamification.domain.UserGamificationStats;
import com.eventitta.domain.gamification.repository.UserActivityStatsRepository;
import com.eventitta.domain.gamification.repository.UserGamificationStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GamificationReconciliationServiceTest {

    @Mock
    private UserGamificationStatsRepository userGamificationStatsRepository;

    @Mock
    private UserActivityStatsRepository userActivityStatsRepository;

    @Mock
    private BadgeService badgeService;

    @Mock
    private RankingService rankingService;

    private GamificationReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        reconciliationService = new GamificationReconciliationService(
            userGamificationStatsRepository,
            userActivityStatsRepository,
            badgeService,
            rankingService
        );
    }

    @Test
    @DisplayName("랭킹 복구는 stats 테이블을 기준으로 두 정렬셋을 다시 채운다")
    void rebuildRankings_rebuildsBothRankingsFromStats() {
        UserGamificationStats user1 = stats(1L, 100, 3);
        UserGamificationStats user2 = stats(2L, 0, 5);

        when(userGamificationStatsRepository.count()).thenReturn(2L);
        when(userGamificationStatsRepository.findAll(PageRequest.of(0, 1000)))
            .thenReturn(new PageImpl<>(List.of(user1, user2)));

        reconciliationService.rebuildRankings();

        verify(rankingService).clearRanking(RankingType.POINTS);
        verify(rankingService).clearRanking(RankingType.ACTIVITY_COUNT);
        verify(rankingService).updateScoresBatch(RankingType.POINTS, Map.of(1L, 100.0));
        verify(rankingService).updateScoresBatch(RankingType.ACTIVITY_COUNT, Map.of(1L, 3.0, 2L, 5.0));
    }

    @Test
    @DisplayName("배지 복구는 action stats 단위로 평가한다")
    void reconcileBadges_replaysPerActionStat() {
        UserActivityStats postStats = actionStats(1L, RewardActionType.CREATE_POST, 2L, 20);
        UserActivityStats commentStats = actionStats(2L, RewardActionType.CREATE_COMMENT, 4L, 20);

        when(userActivityStatsRepository.count()).thenReturn(2L);
        when(userActivityStatsRepository.findAll(PageRequest.of(0, 1000)))
            .thenReturn(new PageImpl<>(List.of(postStats, commentStats)));

        reconciliationService.reconcileBadges();

        verify(badgeService).checkAndAwardBadges(1L, RewardActionType.CREATE_POST, 2L, 20);
        verify(badgeService).checkAndAwardBadges(2L, RewardActionType.CREATE_COMMENT, 4L, 20);
    }

    @Test
    @DisplayName("startup warm-up은 stats 테이블 기준으로 랭킹을 항상 다시 채운다")
    void warmUpRankingsOnStartup_alwaysRebuildsRankings() {
        UserGamificationStats user1 = stats(1L, 30, 2);

        when(userGamificationStatsRepository.count()).thenReturn(1L);
        when(userGamificationStatsRepository.findAll(PageRequest.of(0, 1000)))
            .thenReturn(new PageImpl<>(List.of(user1)));

        reconciliationService.warmUpRankingsOnStartup();

        verify(rankingService).clearRanking(RankingType.POINTS);
        verify(rankingService).clearRanking(RankingType.ACTIVITY_COUNT);
        verify(rankingService).updateScoresBatch(RankingType.POINTS, Map.of(1L, 30.0));
        verify(rankingService).updateScoresBatch(RankingType.ACTIVITY_COUNT, Map.of(1L, 2.0));
    }

    private UserGamificationStats stats(Long userId, int points, long activityCount) {
        UserGamificationStats stats = mock(UserGamificationStats.class);
        when(stats.getUserId()).thenReturn(userId);
        when(stats.getTotalPoints()).thenReturn(points);
        when(stats.getTotalActivityCount()).thenReturn(activityCount);
        return stats;
    }

    private UserActivityStats actionStats(Long userId, RewardActionType actionType, long count, int pointsTotal) {
        UserActivityStats stats = mock(UserActivityStats.class);
        when(stats.getUserId()).thenReturn(userId);
        when(stats.getActionType()).thenReturn(actionType);
        when(stats.getActionCount()).thenReturn(count);
        when(stats.getPointsTotal()).thenReturn(pointsTotal);
        return stats;
    }
}
