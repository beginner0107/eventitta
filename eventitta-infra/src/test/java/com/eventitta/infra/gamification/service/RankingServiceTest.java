package com.eventitta.infra.gamification.service;

import com.eventitta.domain.gamification.domain.RankingType;
import com.eventitta.domain.gamification.domain.UserGamificationStats;
import com.eventitta.domain.gamification.dto.response.RankingPageResponse;
import com.eventitta.domain.gamification.dto.response.UserRankResponse;
import com.eventitta.domain.gamification.repository.UserGamificationStatsRepository;
import com.eventitta.domain.gamification.repository.projection.UserRankingProjection;
import com.eventitta.domain.notification.service.AlertNotificationService;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import com.eventitta.domain.user.api.internal.view.UserProfileView;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("null")
class RankingServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private UserInternalFacade userInternalFacade;

    @Mock
    private UserGamificationStatsRepository userGamificationStatsRepository;

    @Mock
    private AlertNotificationService alertNotificationService;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private RedisRankingService rankingService;

    @BeforeEach
    void setUp() {
        rankingService = new RedisRankingService(
            redisTemplate,
            userInternalFacade,
            userGamificationStatsRepository,
            alertNotificationService
        );

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("Redis 장애 시 포인트 랭킹은 stats 테이블 fallback으로 조회한다")
    void getTopRankings_points_fallbacksToStatsTable() {
        when(zSetOperations.reverseRangeWithScores(anyString(), eq(0L), eq(9L)))
            .thenThrow(new RedisConnectionFailureException("Redis down"));
        when(userGamificationStatsRepository.findTopByPoints(PageRequest.of(0, 10))).thenReturn(List.of(
            projection(1L, "user1", 1000),
            projection(2L, "user2", 900)
        ));
        when(userGamificationStatsRepository.countUsersWithPositivePoints()).thenReturn(2L);

        RankingPageResponse response = rankingService.getTopRankings(RankingType.POINTS, 10);

        assertThat(response.rankings()).hasSize(2);
        assertThat(response.rankings().get(0).userId()).isEqualTo(1L);
        assertThat(response.rankings().get(0).score()).isEqualTo(1000);
        verify(userGamificationStatsRepository).findTopByPoints(PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Redis 장애 시 활동 랭킹은 stats 테이블 fallback으로 조회한다")
    void getTopRankings_activity_fallbacksToStatsTable() {
        when(zSetOperations.reverseRangeWithScores(anyString(), eq(0L), eq(9L)))
            .thenThrow(new RedisConnectionFailureException("Redis down"));
        when(userGamificationStatsRepository.findTopByActivityCount(PageRequest.of(0, 10))).thenReturn(List.of(
            projection(1L, "user1", 20),
            projection(2L, "user2", 15)
        ));
        when(userGamificationStatsRepository.countUsersWithPositiveActivityCount()).thenReturn(2L);

        RankingPageResponse response = rankingService.getTopRankings(RankingType.ACTIVITY_COUNT, 10);

        assertThat(response.rankings()).hasSize(2);
        assertThat(response.rankings().get(0).score()).isEqualTo(20);
        verify(userGamificationStatsRepository).findTopByActivityCount(PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("유저 순위 조회 시 Redis 실패하면 stats 테이블로 순위를 계산한다")
    void getUserRank_fallbacksToStatsTable() {
        Long userId = 1L;
        UserGamificationStats userStats = stats(1000, 10);
        when(zSetOperations.reverseRank(anyString(), eq(userId.toString())))
            .thenThrow(new RedisConnectionFailureException("Redis down"));
        when(userInternalFacade.findUserProfile(userId))
            .thenReturn(Optional.of(new UserProfileView(userId, "user1", null, null, false)));
        when(userGamificationStatsRepository.findById(userId))
            .thenReturn(Optional.of(userStats));
        when(userGamificationStatsRepository.countUsersRankedAheadByPoints(1000, userId))
            .thenReturn(0L);

        UserRankResponse response = rankingService.getUserRank(RankingType.POINTS, userId);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.score()).isEqualTo(1000);
        assertThat(response.rank()).isEqualTo(1);
    }

    @Test
    @DisplayName("Redis가 비어 있으면 전체 사용자 수는 stats 테이블에서 조회한다")
    void getTotalUsers_fallbacksWhenRedisEmpty() {
        when(zSetOperations.zCard(anyString())).thenReturn(0L);
        when(userGamificationStatsRepository.countUsersWithPositivePoints()).thenReturn(5L);

        Long totalUsers = rankingService.getTotalUsers(RankingType.POINTS);

        assertThat(totalUsers).isEqualTo(5L);
    }

    @Test
    @DisplayName("projection 비어 있음 여부는 Redis 정렬셋 기준으로 판단한다")
    void isRankingProjectionEmpty_checksRedisOnly() {
        when(zSetOperations.zCard(anyString())).thenReturn(0L);

        boolean empty = rankingService.isRankingProjectionEmpty(RankingType.POINTS);

        assertThat(empty).isTrue();
    }

    private UserGamificationStats stats(int points, long activityCount) {
        UserGamificationStats stats = mock(UserGamificationStats.class);
        when(stats.getTotalPoints()).thenReturn(points);
        when(stats.getTotalActivityCount()).thenReturn(activityCount);
        return stats;
    }

    private UserRankingProjection projection(Long userId, String nickname, int score) {
        return new UserRankingProjection() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public String getNickname() {
                return nickname;
            }

            @Override
            public String getProfilePictureUrl() {
                return null;
            }

            @Override
            public int getScore() {
                return score;
            }
        };
    }
}
