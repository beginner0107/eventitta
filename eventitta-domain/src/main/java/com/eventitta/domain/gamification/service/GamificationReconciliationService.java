package com.eventitta.domain.gamification.service;

import com.eventitta.domain.gamification.domain.RankingType;
import com.eventitta.domain.gamification.domain.UserActivityStats;
import com.eventitta.domain.gamification.domain.UserGamificationStats;
import com.eventitta.domain.gamification.repository.UserActivityStatsRepository;
import com.eventitta.domain.gamification.repository.UserGamificationStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationReconciliationService {

    private static final int BATCH_SIZE = 1000;

    private final UserGamificationStatsRepository userGamificationStatsRepository;
    private final UserActivityStatsRepository userActivityStatsRepository;
    private final BadgeService badgeService;
    private final RankingService rankingService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpRankingsOnStartup() {
        rebuildRankings();
    }

    @Transactional(readOnly = true)
    public void rebuildRankings() {
        log.info("[GamificationReconciliation] Rebuilding rankings from stats tables");
        rankingService.clearRanking(RankingType.POINTS);
        rankingService.clearRanking(RankingType.ACTIVITY_COUNT);

        long total = userGamificationStatsRepository.count();
        int totalPages = (int) Math.ceil((double) total / BATCH_SIZE);

        for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
            Pageable pageable = PageRequest.of(pageIndex, BATCH_SIZE);
            Page<UserGamificationStats> page = userGamificationStatsRepository.findAll(pageable);

            Map<Long, Double> pointsScores = new HashMap<>();
            Map<Long, Double> activityScores = new HashMap<>();

            for (UserGamificationStats stats : page.getContent()) {
                if (stats.getTotalPoints() > 0) {
                    pointsScores.put(stats.getUserId(), (double) stats.getTotalPoints());
                }
                if (stats.getTotalActivityCount() > 0) {
                    activityScores.put(stats.getUserId(), (double) stats.getTotalActivityCount());
                }
            }

            if (!pointsScores.isEmpty()) {
                rankingService.updateScoresBatch(RankingType.POINTS, pointsScores);
            }
            if (!activityScores.isEmpty()) {
                rankingService.updateScoresBatch(RankingType.ACTIVITY_COUNT, activityScores);
            }
        }
    }

    @Transactional(readOnly = true)
    public void reconcileBadges() {
        log.info("[GamificationReconciliation] Reconciling badges from action stats");

        long total = userActivityStatsRepository.count();
        int totalPages = (int) Math.ceil((double) total / BATCH_SIZE);

        for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
            Pageable pageable = PageRequest.of(pageIndex, BATCH_SIZE);
            Page<UserActivityStats> page = userActivityStatsRepository.findAll(pageable);

            for (UserActivityStats actionStats : page.getContent()) {
                badgeService.checkAndAwardBadges(
                    actionStats.getUserId(),
                    actionStats.getActionType(),
                    actionStats.getActionCount(),
                    actionStats.getPointsTotal()
                );
            }
        }
    }
}
