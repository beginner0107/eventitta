package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.RankingType;
import com.eventitta.gamification.dto.response.RankingPageResponse;
import com.eventitta.gamification.dto.response.UserRankResponse;
import java.util.Map;
public interface RankingService {

    RankingPageResponse getTopRankings(RankingType type, int limit);

    UserRankResponse getUserRank(RankingType type, Long userId);

    void updatePointsRanking(Long userId, int points);

    void updateActivityCountRanking(Long userId, long activityCount);

    void removeUser(RankingType type, Long userId);

    void updateScoresBatch(RankingType type, Map<Long, Double> userScores);

    Long getTotalUsers(RankingType type);
}
