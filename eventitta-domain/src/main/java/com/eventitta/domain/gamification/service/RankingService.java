package com.eventitta.domain.gamification.service;

import com.eventitta.domain.gamification.domain.RankingType;
import com.eventitta.domain.gamification.dto.response.RankingPageResponse;
import com.eventitta.domain.gamification.dto.response.UserRankResponse;
import java.util.Map;
public interface RankingService {

    RankingPageResponse getTopRankings(RankingType type, int limit);

    UserRankResponse getUserRank(RankingType type, Long userId);

    void updatePointsRanking(Long userId, int points);

    void updateActivityCountRanking(Long userId, long activityCount);

    void removeUser(RankingType type, Long userId);

    void clearRanking(RankingType type);

    void updateScoresBatch(RankingType type, Map<Long, Double> userScores);

    Long getTotalUsers(RankingType type);

    boolean isRankingProjectionEmpty(RankingType type);
}
