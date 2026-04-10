package com.eventitta.infra.gamification.service;

import com.eventitta.domain.gamification.domain.RankingType;
import com.eventitta.domain.gamification.dto.response.RankingPageResponse;
import com.eventitta.domain.gamification.dto.response.UserRankResponse;
import com.eventitta.domain.gamification.service.RankingService;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 테스트 프로파일에서 사용하는 no-op RankingService.
 * - Redis/DB 의존 없이 ApplicationContext 로딩을 안정화한다.
 * - Redis 장애/미구동 환경에서도 애플리케이션 기능의 나머지가 동작하도록 설계 가능.
 */
@Service
@Profile("test")
@Primary
public class NoopRankingService implements RankingService {

    @Override
    public RankingPageResponse getTopRankings(RankingType type, int limit) {
        return new RankingPageResponse(List.of(), 0L, type);
    }

    @Override
    public UserRankResponse getUserRank(RankingType type, Long userId) {
        return new UserRankResponse(userId, "test", null, 0, 1);
    }

    @Override
    public void updatePointsRanking(Long userId, int points) {
    }

    @Override
    public void updateActivityCountRanking(Long userId, long activityCount) {
    }

    @Override
    public void removeUser(RankingType type, Long userId) {
    }

    @Override
    public void clearRanking(RankingType type) {
    }

    @Override
    public void updateScoresBatch(RankingType type, Map<Long, Double> userScores) {
    }

    @Override
    public Long getTotalUsers(RankingType type) {
        return 0L;
    }

    @Override
    public boolean isRankingProjectionEmpty(RankingType type) {
        return true;
    }
}
