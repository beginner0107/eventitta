package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.RankingType;
import com.eventitta.gamification.dto.response.RankingPageResponse;
import com.eventitta.gamification.dto.response.UserRankResponse;
import com.eventitta.gamification.exception.RankingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.eventitta.gamification.exception.RankingErrorCode.RANKING_NOT_FOUND;

/**
 * 테스트용 RankingService Mock 구현체
 * Redis 없이 메모리 기반으로 동작
 */
@Service("rankingService")
@Profile("test")
public class MockRankingService implements RankingService {

    private static final Logger log = LoggerFactory.getLogger(MockRankingService.class);
    private final Map<String, Map<Long, Double>> rankings = new ConcurrentHashMap<>();

    public MockRankingService() {
        // 각 랭킹 타입별로 빈 맵 초기화
        for (RankingType type : RankingType.values()) {
            rankings.put(type.name(), new ConcurrentHashMap<>());
        }
    }

    @Override
    public void updatePointsRanking(Long userId, int points) {
        log.debug("Mock updating points ranking - userId: {}, points: {}", userId, points);
        if (points > 0) {
            rankings.get(RankingType.POINTS.name()).put(userId, (double) points);
        } else {
            rankings.get(RankingType.POINTS.name()).remove(userId);
        }
    }

    @Override
    public void updateActivityCountRanking(Long userId, long activityCount) {
        log.debug("Mock updating activity ranking - userId: {}, count: {}", userId, activityCount);
        if (activityCount > 0) {
            rankings.get(RankingType.ACTIVITY_COUNT.name()).put(userId, (double) activityCount);
        } else {
            rankings.get(RankingType.ACTIVITY_COUNT.name()).remove(userId);
        }
    }

    @Override
    public void updateScoresBatch(RankingType type, Map<Long, Double> scores) {
        log.debug("Mock batch updating {} ranking with {} scores", type, scores.size());
        Map<Long, Double> typeRankings = rankings.get(type.name());
        scores.forEach((userId, score) -> {
            if (score > 0) {
                typeRankings.put(userId, score);
            } else {
                typeRankings.remove(userId);
            }
        });
    }

    @Override
    public void removeUser(RankingType type, Long userId) {
        log.debug("Mock removing user {} from {} ranking", userId, type);
        rankings.get(type.name()).remove(userId);
    }

    @Override
    public RankingPageResponse getTopRankings(RankingType type, int limit) {
        Map<Long, Double> typeRankings = rankings.get(type.name());

        List<UserRankResponse> topList = typeRankings.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> {
                int rank = getRank(type, entry.getKey());
                return new UserRankResponse(
                    entry.getKey(),
                    "user" + entry.getKey(),
                    null,
                    entry.getValue().intValue(),
                    rank
                );
            })
            .toList();

        return new RankingPageResponse(topList, (long) typeRankings.size(), type);
    }

    @Override
    public UserRankResponse getUserRank(RankingType type, Long userId) {
        Map<Long, Double> typeRankings = rankings.get(type.name());
        Double score = typeRankings.get(userId);

        if (score == null) {
            throw new RankingException(RANKING_NOT_FOUND);
        }

        int rank = getRank(type, userId);
        return new UserRankResponse(
            userId,
            "user" + userId,
            null,
            score.intValue(),
            rank
        );
    }

    @Override
    public Long getTotalUsers(RankingType type) {
        return (long) rankings.get(type.name()).size();
    }

    public void evictUserRankCache(RankingType type, Long userId) {
        log.debug("Mock evicting cache for user {} in {} ranking", userId, type);
    }

    private int getRank(RankingType type, Long userId) {
        Map<Long, Double> typeRankings = rankings.get(type.name());
        Double userScore = typeRankings.get(userId);
        if (userScore == null) return 0;

        long higherCount = typeRankings.values().stream()
            .filter(score -> score > userScore)
            .count();

        return (int) (higherCount + 1);
    }
}
