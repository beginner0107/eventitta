package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.RankingType;
import com.eventitta.gamification.dto.response.RankingPageResponse;
import com.eventitta.gamification.dto.response.UserRankResponse;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.notification.domain.AlertLevel;
import com.eventitta.notification.service.DiscordNotificationService;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

@Slf4j
@Service
@Profile("!test")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class RedisRankingService implements RankingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final UserActivityRepository userActivityRepository;
    private final DiscordNotificationService discordNotificationService;

    /**
     * Top N 순위 조회
     * Redis 실패 시 간단한 MySQL Fallback
     */
    @Override
    @Transactional(readOnly = true)
    public RankingPageResponse getTopRankings(RankingType type, int limit) {
        try {
            return getTopRankingsFromRedis(type, limit);
        } catch (Exception e) {
            log.error("Redis failed, fallback to MySQL. type={}, error={}", type, e.getMessage());
            return getTopRankingsFromDatabase(type, limit);
        }
    }

    private RankingPageResponse getTopRankingsFromRedis(RankingType type, int limit) {
        String redisKey = Objects.requireNonNull(type.getRedisKey());
        Set<ZSetOperations.TypedTuple<Object>> rankings =
            redisTemplate.opsForZSet().reverseRangeWithScores(
                redisKey, 0, limit - 1
            );

        if (rankings == null || rankings.isEmpty()) {
            return new RankingPageResponse(List.of(), 0L, type);
        }

        List<Long> userIds = rankings.stream()
            .map(ZSetOperations.TypedTuple::getValue)
            .filter(Objects::nonNull)
            .map(value -> Long.parseLong(value.toString()))
            .toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, user -> user));

        long rank = 1;
        List<UserRankResponse> responses = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> tuple : rankings) {
            Object value = tuple.getValue();
            Double score = tuple.getScore();
            if (value == null || score == null) {
                continue;
            }

            Long userId = Long.parseLong(value.toString());
            User user = userMap.get(userId);

            if (user != null) {
                responses.add(new UserRankResponse(
                    userId,
                    user.getNickname(),
                    user.getProfilePictureUrl(),
                    score.intValue(),
                    rank++
                ));
            }
        }

        Long totalUsers = redisTemplate.opsForZSet().zCard(redisKey);
        return new RankingPageResponse(responses, totalUsers != null ? totalUsers : 0L, type);
    }

    private RankingPageResponse getTopRankingsFromDatabase(RankingType type, int limit) {
        List<UserRankResponse> responses = new ArrayList<>();
        long totalUsers = userRepository.count();

        if (type == RankingType.POINTS) {
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
     * Caffeine 캐시를 제거하고 Redis만 사용하여 분산 환경 정합성 보장
     */
    @Override
    @Transactional(readOnly = true)
    public UserRankResponse getUserRank(RankingType type, Long userId) {
        try {
            return getUserRankFromRedis(type, userId);
        } catch (RedisConnectionFailureException e) {
            // Redis 완전 장애: Critical 알림 + MySQL Fallback
            log.error("Redis connection failed for user rank, using MySQL fallback. userId={}, type={}, error={}",
                userId, type, e.getMessage());
            discordNotificationService.sendAlert(
                AlertLevel.CRITICAL,
                "REDIS_CONNECTION_FAILURE",
                "Redis connection failed while getting user rank",
                "/api/v1/rankings/user",
                "userId=" + userId + ", type=" + type,
                e
            );
            return getUserRankFromDatabase(type, userId);
        } catch (Exception e) {
            // 기타 예외 (타임아웃 포함): MySQL Fallback
            log.warn("Redis error for user rank, using MySQL fallback. userId={}, type={}, error={}",
                userId, type, e.getMessage());
            return getUserRankFromDatabase(type, userId);
        }
    }

    private UserRankResponse getUserRankFromRedis(RankingType type, Long userId) {
        String redisKey = Objects.requireNonNull(type.getRedisKey());
        String member = Objects.requireNonNull(userId).toString();

        Long rank = redisTemplate.opsForZSet().reverseRank(redisKey, member);

        if (rank == null) {
            log.warn("User not found in ranking. type={}, userId={}", type, userId);
            return getUserRankFromDatabase(type, userId);
        }

        Double score = redisTemplate.opsForZSet().score(redisKey, member);
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
            rank = 1;
        }

        return new UserRankResponse(
            userId,
            user.getNickname(),
            user.getProfilePictureUrl(),
            score,
            rank
        );
    }

    @Override
    public void updatePointsRanking(Long userId, int points) {
        try {
            String member = Objects.requireNonNull(userId).toString();
            redisTemplate.opsForZSet().add(
                RankingType.POINTS.getRedisKey(),
                member,
                points
            );
        } catch (Exception e) {
            log.error("Failed to update points ranking. userId={}, points={}", userId, points, e);
        }
    }

    @Override
    public void updateActivityCountRanking(Long userId, long activityCount) {
        try {
            String member = Objects.requireNonNull(userId).toString();
            redisTemplate.opsForZSet().add(
                RankingType.ACTIVITY_COUNT.getRedisKey(),
                member,
                (double) activityCount
            );
        } catch (Exception e) {
            log.error("Failed to update activity ranking. userId={}, count={}", userId, activityCount, e);
        }
    }


    @Override
    public void removeUser(RankingType type, Long userId) {
        try {
            String redisKey = Objects.requireNonNull(type.getRedisKey());
            String member = Objects.requireNonNull(userId).toString();
            redisTemplate.opsForZSet().remove(redisKey, member);
        } catch (Exception e) {
            log.error("Failed to remove user from ranking. type={}, userId={}", type, userId, e);
        }
    }

    @Override
    public void updateScoresBatch(RankingType type, Map<Long, Double> userScores) {
        try {
            String redisKey = Objects.requireNonNull(type.getRedisKey());
            for (Map.Entry<Long, Double> entry : userScores.entrySet()) {
                redisTemplate.opsForZSet().add(
                    redisKey,
                    Objects.requireNonNull(entry.getKey()).toString(),
                    entry.getValue()
                );
            }
            log.info("Batch updated {} scores for type: {}", userScores.size(), type);
        } catch (Exception e) {
            log.error("Failed to batch update scores. type={}, size={}", type, userScores.size(), e);
        }
    }

    @Override
    public Long getTotalUsers(RankingType type) {
        try {
            String redisKey = Objects.requireNonNull(type.getRedisKey());
            return redisTemplate.opsForZSet().zCard(redisKey);
        } catch (Exception e) {
            log.error("Failed to get total users. type={}", type, e);
            return 0L;
        }
    }
}
