package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.RankingType;
import com.eventitta.gamification.dto.response.RankingPageResponse;
import com.eventitta.gamification.dto.response.UserRankResponse;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final UserActivityRepository userActivityRepository;

    /**
     * Top N 순위 조회
     * Redis 실패 시 간단한 MySQL Fallback
     */
    @Transactional(readOnly = true)
    public RankingPageResponse getTopRankings(RankingType type, int limit) {
        try {
            // Redis에서 조회
            return getTopRankingsFromRedis(type, limit);
        } catch (Exception e) {
            // 간단한 Fallback
            log.error("Redis failed, fallback to MySQL. type={}, error={}", type, e.getMessage());
            return getTopRankingsFromDatabase(type, limit);
        }
    }

    /**
     * Redis에서 순위 조회
     */
    private RankingPageResponse getTopRankingsFromRedis(RankingType type, int limit) {
        Set<ZSetOperations.TypedTuple<Object>> rankings =
            redisTemplate.opsForZSet().reverseRangeWithScores(
                type.getRedisKey(), 0, limit - 1
            );

        if (rankings == null || rankings.isEmpty()) {
            return new RankingPageResponse(List.of(), 0L, type);
        }

        List<Long> userIds = rankings.stream()
            .map(tuple -> Long.parseLong(tuple.getValue().toString()))
            .toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, user -> user));

        long rank = 1;
        List<UserRankResponse> responses = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> tuple : rankings) {
            Long userId = Long.parseLong(tuple.getValue().toString());
            User user = userMap.get(userId);

            if (user != null) {
                responses.add(new UserRankResponse(
                    userId,
                    user.getNickname(),
                    user.getProfilePictureUrl(),
                    tuple.getScore().intValue(),
                    rank++
                ));
            }
        }

        Long totalUsers = redisTemplate.opsForZSet().zCard(type.getRedisKey());
        return new RankingPageResponse(responses, totalUsers != null ? totalUsers : 0L, type);
    }

    /**
     * MySQL에서 순위 조회 (간단한 Fallback)
     */
    private RankingPageResponse getTopRankingsFromDatabase(RankingType type, int limit) {
        List<UserRankResponse> responses = new ArrayList<>();
        long totalUsers = userRepository.count();

        if (type == RankingType.POINTS) {
            // 포인트 순위
            List<User> topUsers = userRepository.findTopUsersByPoints(PageRequest.of(0, limit));

            long rank = 1;
            for (User user : topUsers) {
                responses.add(new UserRankResponse(
                    user.getId(),
                    user.getNickname(),
                    user.getProfilePictureUrl(),
                    user.getPoints(),
                    rank++
                ));
            }
        } else if (type == RankingType.ACTIVITY_COUNT) {
            List<Long> userIds = userActivityRepository.findRecentlyActiveUserIds(168);
            if (!userIds.isEmpty()) {
                List<User> users = userRepository.findAllById(userIds.subList(0, Math.min(limit, userIds.size())));

                long rank = 1;
                for (User user : users) {
                    long activityCount = userActivityRepository.countByUserId(user.getId());
                    responses.add(new UserRankResponse(
                        user.getId(),
                        user.getNickname(),
                        user.getProfilePictureUrl(),
                        (int) activityCount,
                        rank++
                    ));
                }
            }
        }

        return new RankingPageResponse(responses, totalUsers, type);
    }

    /**
     * 특정 유저의 순위 조회
     */
    @Cacheable(value = "userRank", key = "#type + ':' + #userId", unless = "#result == null")
    @Transactional(readOnly = true)
    public UserRankResponse getUserRank(RankingType type, Long userId) {
        try {
            // Redis에서 조회
            return getUserRankFromRedis(type, userId);
        } catch (Exception e) {
            log.error("Redis failed for user rank, fallback to MySQL. userId={}, error={}",
                userId, e.getMessage());
            return getUserRankFromDatabase(type, userId);
        }
    }

    /**
     * Redis에서 유저 순위 조회
     */
    private UserRankResponse getUserRankFromRedis(RankingType type, Long userId) {
        Long rank = redisTemplate.opsForZSet().reverseRank(type.getRedisKey(), userId.toString());

        if (rank == null) {
            log.warn("User not found in ranking. type={}, userId={}", type, userId);
            return getUserRankFromDatabase(type, userId);
        }

        Double score = redisTemplate.opsForZSet().score(type.getRedisKey(), userId.toString());
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        return new UserRankResponse(
            userId,
            user.getNickname(),
            user.getProfilePictureUrl(),
            score != null ? score.intValue() : 0,
            rank + 1
        );
    }

    /**
     * MySQL에서 유저 순위 조회
     */
    private UserRankResponse getUserRankFromDatabase(RankingType type, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        int score = 0;
        long rank = 1;

        if (type == RankingType.POINTS) {
            score = user.getPoints();
            rank = userRepository.countByPointsGreaterThan(score) + 1;
        } else if (type == RankingType.ACTIVITY_COUNT) {
            score = (int) userActivityRepository.countByUserId(userId);
            rank = 1; // 또는 예상 순위
        }

        return new UserRankResponse(
            userId,
            user.getNickname(),
            user.getProfilePictureUrl(),
            score,
            rank
        );
    }

    /**
     * 포인트 순위 업데이트 (전체 기간만)
     */
    public void updatePointsRanking(Long userId, int points) {
        try {
            redisTemplate.opsForZSet().add(
                RankingType.POINTS.getRedisKey(),
                userId.toString(),
                points
            );
            evictUserRankCache(RankingType.POINTS, userId);
        } catch (Exception e) {
            log.error("Failed to update points ranking. userId={}, points={}", userId, points, e);
        }
    }

    /**
     * 활동량 순위 업데이트 (전체 기간만)
     */
    public void updateActivityCountRanking(Long userId, long activityCount) {
        try {
            redisTemplate.opsForZSet().add(
                RankingType.ACTIVITY_COUNT.getRedisKey(),
                userId.toString(),
                (double) activityCount
            );
            evictUserRankCache(RankingType.ACTIVITY_COUNT, userId);
        } catch (Exception e) {
            log.error("Failed to update activity ranking. userId={}, count={}", userId, activityCount, e);
        }
    }

    /**
     * 유저 순위 캐시 무효화
     */
    @CacheEvict(value = "userRank", key = "#type + ':' + #userId")
    public void evictUserRankCache(RankingType type, Long userId) {
    }

    /**
     * 유저를 순위에서 제거
     */
    public void removeUser(RankingType type, Long userId) {
        try {
            redisTemplate.opsForZSet().remove(type.getRedisKey(), userId.toString());
            evictUserRankCache(type, userId);
        } catch (Exception e) {
            log.error("Failed to remove user from ranking. type={}, userId={}", type, userId, e);
        }
    }

    /**
     * 배치로 여러 유저 점수 업데이트
     */
    public void updateScoresBatch(RankingType type, Map<Long, Double> userScores) {
        try {
            for (Map.Entry<Long, Double> entry : userScores.entrySet()) {
                redisTemplate.opsForZSet().add(
                    type.getRedisKey(),
                    entry.getKey().toString(),
                    entry.getValue()
                );
            }
            log.info("Batch updated {} scores for type: {}", userScores.size(), type);
        } catch (Exception e) {
            log.error("Failed to batch update scores. type={}, size={}", type, userScores.size(), e);
        }
    }

    /**
     * 전체 유저 수 조회
     */
    public Long getTotalUsers(RankingType type) {
        try {
            return redisTemplate.opsForZSet().zCard(type.getRedisKey());
        } catch (Exception e) {
            log.error("Failed to get total users. type={}", type, e);
            return 0L;
        }
    }
}
