package com.eventitta.gamification.service;

import com.eventitta.gamification.constants.RankingConstants;
import com.eventitta.gamification.domain.RankingType;
import com.eventitta.gamification.dto.response.RankingPageResponse;
import com.eventitta.gamification.dto.response.UserRankResponse;
import com.eventitta.gamification.exception.RankingErrorCode;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

/**
 * 순위 시스템 서비스
 * Redis zSet을 이용한 실시간 순위 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    /**
     * Top N 순위 조회 (유저 정보 포함)
     *
     * @param type  순위 타입 (POINTS 또는 ACTIVITY_COUNT)
     * @param limit 조회할 순위 수
     * @return 순위 페이지 응답
     */
    @Transactional(readOnly = true)
    public RankingPageResponse getTopRankings(RankingType type, int limit) {
        try {
            // Redis에서 Top N 순위 조회 (내림차순)
            Set<ZSetOperations.TypedTuple<Object>> rankings =
                redisTemplate.opsForZSet().reverseRangeWithScores(
                    type.getRedisKey(), 0, limit - 1
                );

            if (rankings == null || rankings.isEmpty()) {
                log.debug("[Ranking] No rankings found for type: {}", type);
                return new RankingPageResponse(List.of(), 0L, type);
            }

            // userId 목록 추출
            List<Long> userIds = rankings.stream()
                .map(tuple -> {
                    if (tuple.getValue() instanceof String) {
                        return Long.parseLong((String) tuple.getValue());
                    }
                    return (Long) tuple.getValue();
                })
                .toList();

            // User 정보 조회
            Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

            // UserRankResponse 목록 생성
            long rank = 1;
            List<UserRankResponse> userRankResponses = new ArrayList<>();
            for (ZSetOperations.TypedTuple<Object> tuple : rankings) {
                Long userId = tuple.getValue() instanceof String
                    ? Long.valueOf(Long.parseLong((String) tuple.getValue()))
                    : (Long) tuple.getValue();
                User user = userMap.get(userId);

                if (user != null) {
                    userRankResponses.add(new UserRankResponse(
                        userId,
                        user.getNickname(),
                        user.getProfilePictureUrl(),
                        tuple.getScore() != null ? tuple.getScore().intValue() : 0,
                        rank++
                    ));
                } else {
                    log.warn("[Ranking] User not found in database: userId={}", userId);
                }
            }

            // 전체 유저 수 조회
            Long totalUsers = redisTemplate.opsForZSet().zCard(type.getRedisKey());

            return new RankingPageResponse(
                userRankResponses,
                totalUsers != null ? totalUsers : 0L,
                type
            );

        } catch (Exception e) {
            log.error("[Ranking] Failed to get top rankings. type={}, limit={}",
                type, limit, e);
            throw RankingErrorCode.REDIS_CONNECTION_FAILED.defaultException(e);
        }
    }

    /**
     * 특정 유저의 순위 조회
     * 캐시를 사용하여 Redis 부하 감소
     *
     * @param type   순위 타입
     * @param userId 유저 ID
     * @return 유저 순위 응답
     */
    @Cacheable(value = "userRank", key = "#type + ':' + #userId", unless = "#result == null")
    @Transactional(readOnly = true)
    public UserRankResponse getUserRank(RankingType type, Long userId) {
        try {
            // Redis에서 순위 조회 (0-based index)
            Long rank = redisTemplate.opsForZSet().reverseRank(type.getRedisKey(), userId.toString());

            if (rank == null) {
                log.debug("[Ranking] User not found in ranking. type={}, userId={}", type, userId);
                throw RankingErrorCode.RANKING_NOT_FOUND.defaultException();
            }

            // Redis에서 점수 조회
            Double score = redisTemplate.opsForZSet().score(type.getRedisKey(), userId.toString());

            // User 정보 조회
            User user = userRepository.findById(userId)
                .orElseThrow(NOT_FOUND_USER_ID::defaultException);

            return new UserRankResponse(
                userId,
                user.getNickname(),
                user.getProfilePictureUrl(),
                score != null ? score.intValue() : 0,
                rank + 1  // 1-based rank로 변환
            );

        } catch (Exception e) {
            if (e instanceof com.eventitta.common.exception.CustomException) {
                throw e;
            }
            log.error("[Ranking] Failed to get user rank. type={}, userId={}",
                type, userId, e);
            throw RankingErrorCode.REDIS_CONNECTION_FAILED.defaultException(e);
        }
    }

    /**
     * 포인트 순위 업데이트
     *
     * @param userId 유저 ID
     * @param points 새로운 포인트 값
     */
    public void updatePointsRanking(Long userId, int points) {
        try {
            redisTemplate.opsForZSet().add(
                RankingType.POINTS.getRedisKey(),
                userId.toString(),
                (double) points
            );
            evictUserRankCache(RankingType.POINTS, userId);
            log.debug("[Ranking] Updated points ranking. userId={}, points={}", userId, points);
        } catch (Exception e) {
            log.error("[Ranking] Failed to update points ranking. userId={}, points={}",
                userId, points, e);
            throw RankingErrorCode.RANKING_UPDATE_FAILED.defaultException(e);
        }
    }

    /**
     * 활동량 순위 업데이트
     *
     * @param userId        유저 ID
     * @param activityCount 활동 수
     */
    public void updateActivityCountRanking(Long userId, long activityCount) {
        try {
            redisTemplate.opsForZSet().add(
                RankingType.ACTIVITY_COUNT.getRedisKey(),
                userId.toString(),
                (double) activityCount
            );
            evictUserRankCache(RankingType.ACTIVITY_COUNT, userId);
            log.debug("[Ranking] Updated activity count ranking. userId={}, count={}",
                userId, activityCount);
        } catch (Exception e) {
            log.error("[Ranking] Failed to update activity count ranking. userId={}, count={}",
                userId, activityCount, e);
            throw RankingErrorCode.RANKING_UPDATE_FAILED.defaultException(e);
        }
    }

    /**
     * 배치로 여러 유저 점수 업데이트
     *
     * @param type       순위 타입
     * @param userScores 유저별 점수 맵
     */
    public void updateScoresBatch(RankingType type, Map<Long, Double> userScores) {
        try {
            Set<ZSetOperations.TypedTuple<Object>> tuples = userScores.entrySet().stream()
                .map(entry -> ZSetOperations.TypedTuple.of(
                    (Object) entry.getKey().toString(),
                    entry.getValue()
                ))
                .collect(Collectors.toSet());

            redisTemplate.opsForZSet().add(type.getRedisKey(), tuples);

            // 배치 업데이트 시 모든 관련 캐시 무효화
            userScores.keySet().forEach(userId ->
                evictUserRankCache(type, userId)
            );

            log.info("[Ranking] Batch updated {} scores for type: {}",
                userScores.size(), type);
        } catch (Exception e) {
            log.error("[Ranking] Failed to batch update scores. type={}, size={}",
                type, userScores.size(), e);
            throw RankingErrorCode.RANKING_UPDATE_FAILED.defaultException(e);
        }
    }

    /**
     * 유저 순위 캐시 무효화
     *
     * @param type   순위 타입
     * @param userId 유저 ID
     */
    @CacheEvict(value = "userRank", key = "#type + ':' + #userId")
    public void evictUserRankCache(RankingType type, Long userId) {
        log.trace("[Ranking] Evicted user rank cache. type={}, userId={}", type, userId);
    }

    /**
     * 특정 유저를 순위에서 제거
     *
     * @param type   순위 타입
     * @param userId 유저 ID
     */
    public void removeUser(RankingType type, Long userId) {
        try {
            Long removed = redisTemplate.opsForZSet().remove(
                type.getRedisKey(),
                userId.toString()
            );

            if (removed != null && removed > 0) {
                evictUserRankCache(type, userId);
                log.info("[Ranking] Removed user from ranking. type={}, userId={}", type, userId);
            }
        } catch (Exception e) {
            log.error("[Ranking] Failed to remove user from ranking. type={}, userId={}",
                type, userId, e);
        }
    }

    /**
     * 전체 유저 수 조회
     *
     * @param type 순위 타입
     * @return 전체 유저 수
     */
    public Long getTotalUsers(RankingType type) {
        try {
            return redisTemplate.opsForZSet().zCard(type.getRedisKey());
        } catch (Exception e) {
            log.error("[Ranking] Failed to get total users. type={}", type, e);
            return 0L;
        }
    }
}
