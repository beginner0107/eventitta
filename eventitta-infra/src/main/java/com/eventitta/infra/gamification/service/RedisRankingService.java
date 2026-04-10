package com.eventitta.infra.gamification.service;

import com.eventitta.domain.gamification.domain.RankingType;
import com.eventitta.domain.gamification.domain.UserGamificationStats;
import com.eventitta.domain.gamification.dto.response.RankingPageResponse;
import com.eventitta.domain.gamification.dto.response.UserRankResponse;
import com.eventitta.domain.gamification.repository.UserGamificationStatsRepository;
import com.eventitta.domain.gamification.repository.projection.UserRankingProjection;
import com.eventitta.domain.gamification.service.RankingService;
import com.eventitta.domain.notification.domain.AlertLevel;
import com.eventitta.domain.notification.service.AlertNotificationService;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import com.eventitta.domain.user.api.internal.view.UserProfileView;
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

import static com.eventitta.domain.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

@Slf4j
@Service
@Profile("!test")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class RedisRankingService implements RankingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserInternalFacade userInternalFacade;
    private final UserGamificationStatsRepository userGamificationStatsRepository;
    private final AlertNotificationService alertNotificationService;

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
            return getTopRankingsFromDatabase(type, limit);
        }

        List<Long> userIds = rankings.stream()
            .map(ZSetOperations.TypedTuple::getValue)
            .filter(Objects::nonNull)
            .map(value -> Long.parseLong(value.toString()))
            .toList();

        Map<Long, UserProfileView> userMap = userInternalFacade.findUserProfiles(userIds);

        long rank = 1;
        List<UserRankResponse> responses = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> tuple : rankings) {
            Object value = tuple.getValue();
            Double score = tuple.getScore();
            if (value == null || score == null) {
                continue;
            }

            Long userId = Long.parseLong(value.toString());
            UserProfileView user = userMap.get(userId);
            if (user != null && user.isActive()) {
                responses.add(new UserRankResponse(
                    userId,
                    user.nickname(),
                    user.profilePictureUrl(),
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
        long totalUsers;

        if (type == RankingType.POINTS) {
            List<UserRankingProjection> topUsers =
                userGamificationStatsRepository.findTopByPoints(PageRequest.of(0, limit));
            totalUsers = userGamificationStatsRepository.countUsersWithPositivePoints();

            long rank = 1;
            for (UserRankingProjection user : topUsers) {
                responses.add(new UserRankResponse(
                    user.getUserId(),
                    user.getNickname(),
                    user.getProfilePictureUrl(),
                    user.getScore(),
                    rank++
                ));
            }
        } else {
            List<UserRankingProjection> topUsers =
                userGamificationStatsRepository.findTopByActivityCount(PageRequest.of(0, limit));
            totalUsers = userGamificationStatsRepository.countUsersWithPositiveActivityCount();

            long rank = 1;
            for (UserRankingProjection user : topUsers) {
                responses.add(new UserRankResponse(
                    user.getUserId(),
                    user.getNickname(),
                    user.getProfilePictureUrl(),
                    user.getScore(),
                    rank++
                ));
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
            alertNotificationService.sendAlert(
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
        UserProfileView user = userInternalFacade.findUserProfile(userId)
            .filter(UserProfileView::isActive)
            .orElseThrow(() -> {
                removeUser(type, userId);
                return NOT_FOUND_USER_ID.defaultException();
            });

        return new UserRankResponse(
            userId,
            user.nickname(),
            user.profilePictureUrl(),
            score != null ? score.intValue() : 0,
            rank + 1
        );
    }

    private UserRankResponse getUserRankFromDatabase(RankingType type, Long userId) {
        UserProfileView user = userInternalFacade.findUserProfile(userId)
            .filter(UserProfileView::isActive)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        UserGamificationStats stats = userGamificationStatsRepository.findById(userId).orElse(null);
        int score = 0;
        long rank = 1;

        if (type == RankingType.POINTS) {
            score = stats != null ? stats.getTotalPoints() : 0;
            rank = score > 0
                ? userGamificationStatsRepository.countUsersRankedAheadByPoints(score, userId) + 1
                : userGamificationStatsRepository.countUsersWithPositivePoints() + 1;
        } else if (type == RankingType.ACTIVITY_COUNT) {
            long activityCount = stats != null ? stats.getTotalActivityCount() : 0L;
            score = Math.toIntExact(activityCount);
            rank = activityCount > 0
                ? userGamificationStatsRepository.countUsersRankedAheadByActivityCount(activityCount, userId) + 1
                : userGamificationStatsRepository.countUsersWithPositiveActivityCount() + 1;
        }

        return new UserRankResponse(
            userId,
            user.nickname(),
            user.profilePictureUrl(),
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
    public void clearRanking(RankingType type) {
        try {
            String redisKey = Objects.requireNonNull(type.getRedisKey());
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("Failed to clear ranking. type={}", type, e);
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
            Long totalUsers = redisTemplate.opsForZSet().zCard(redisKey);
            if (totalUsers != null && totalUsers > 0) {
                return totalUsers;
            }
            return getTotalUsersFromDatabase(type);
        } catch (Exception e) {
            log.error("Failed to get total users. type={}", type, e);
            return getTotalUsersFromDatabase(type);
        }
    }

    private long getTotalUsersFromDatabase(RankingType type) {
        if (type == RankingType.POINTS) {
            return userGamificationStatsRepository.countUsersWithPositivePoints();
        }
        return userGamificationStatsRepository.countUsersWithPositiveActivityCount();
    }

    @Override
    public boolean isRankingProjectionEmpty(RankingType type) {
        try {
            String redisKey = Objects.requireNonNull(type.getRedisKey());
            Long totalUsers = redisTemplate.opsForZSet().zCard(redisKey);
            return totalUsers == null || totalUsers == 0L;
        } catch (Exception e) {
            log.warn("Failed to inspect Redis ranking projection. type={}, error={}", type, e.getMessage());
            return false;
        }
    }
}
