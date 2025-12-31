package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.RankingType;
import com.eventitta.gamification.dto.response.RankingPageResponse;
import com.eventitta.gamification.dto.response.UserRankResponse;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserActivityRepository userActivityRepository;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private RankingService rankingService;

    @BeforeEach
    void setUp() {
        rankingService = new RankingService(
            redisTemplate,
            userRepository,
            userActivityRepository
        );

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("Redis 장애 시 MySQL Fallback이 동작한다")
    void testFallbackToMySQLWhenRedisDown() {
        // given
        when(zSetOperations.reverseRangeWithScores(anyString(), eq(0L), eq(9L)))
            .thenThrow(new RedisConnectionFailureException("Redis connection failed"));

        User user1 = createUser(1L, "user1", 1000);
        User user2 = createUser(2L, "user2", 900);

        when(userRepository.findTopUsersByPoints(PageRequest.of(0, 10)))
            .thenReturn(List.of(user1, user2));
        when(userRepository.count()).thenReturn(2L);

        // when
        RankingPageResponse response = rankingService.getTopRankings(RankingType.POINTS, 10);

        // then
        assertThat(response.rankings()).hasSize(2);
        assertThat(response.rankings().get(0).userId()).isEqualTo(1L);
        assertThat(response.rankings().get(0).score()).isEqualTo(1000);

        // MySQL 호출 확인
        verify(userRepository).findTopUsersByPoints(any(PageRequest.class));
    }

    @Test
    @DisplayName("Redis 정상 동작 시 올바르게 조회된다")
    void testRedisWorksCorrectly() {
        // given
        when(zSetOperations.reverseRangeWithScores(anyString(), eq(0L), eq(9L)))
            .thenReturn(Set.of());

        // when
        RankingPageResponse response = rankingService.getTopRankings(RankingType.POINTS, 10);

        // then
        assertThat(response.rankings()).isEmpty();

        // Redis 호출 확인
        verify(zSetOperations).reverseRangeWithScores(anyString(), eq(0L), eq(9L));
    }

    @Test
    @DisplayName("활동량 순위 조회 시 Fallback이 동작한다")
    void testActivityRankingFallback() {
        // given
        when(zSetOperations.reverseRangeWithScores(anyString(), eq(0L), eq(9L)))
            .thenThrow(new RedisConnectionFailureException("Redis down"));

        List<Long> activeUserIds = List.of(1L, 2L, 3L);
        when(userActivityRepository.findRecentlyActiveUserIds(168))
            .thenReturn(activeUserIds);

        User user1 = createUser(1L, "user1", 100);
        User user2 = createUser(2L, "user2", 200);
        User user3 = createUser(3L, "user3", 150);

        when(userRepository.findAllById(anyList()))
            .thenReturn(List.of(user1, user2, user3));

        when(userActivityRepository.countByUserId(1L)).thenReturn(10L);
        when(userActivityRepository.countByUserId(2L)).thenReturn(20L);
        when(userActivityRepository.countByUserId(3L)).thenReturn(15L);

        // when
        RankingPageResponse response = rankingService.getTopRankings(RankingType.ACTIVITY_COUNT, 10);

        // then
        assertThat(response.rankings()).hasSize(3);

        // MySQL Fallback이 호출됨
        verify(userActivityRepository).findRecentlyActiveUserIds(168);
    }

    @Test
    @DisplayName("유저 순위 조회 시 Redis 실패하면 MySQL에서 조회한다")
    void testGetUserRankFallback() {
        // given
        Long userId = 1L;
        when(zSetOperations.reverseRank(anyString(), eq(userId.toString())))
            .thenThrow(new RedisConnectionFailureException("Redis down"));

        User user = createUser(userId, "user1", 1000);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // MySQL fallback 동작
        when(userRepository.countByPointsGreaterThan(1000)).thenReturn(0L);

        // when
        UserRankResponse response = rankingService.getUserRank(RankingType.POINTS, userId);

        // then
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.score()).isEqualTo(1000);
        assertThat(response.rank()).isEqualTo(1); // 최고 점수이므로 1위
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
