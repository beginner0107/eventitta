package com.eventitta.gamification.scheduler;

import com.eventitta.gamification.service.RankingSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 순위 동기화 스케줄러
 * MySQL → Redis 동기화를 정기적으로 수행
 */
@Slf4j
@Component
@Profile("!test")  // 테스트 프로파일에서 비활성화
@RequiredArgsConstructor
public class RankingScheduler {

    private final RankingSyncService rankingSyncService;

    /**
     * 애플리케이션 시작 시 초기 동기화 수행
     * Redis가 비어있을 수 있으므로 전체 동기화 실행
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("[RankingScheduler] Application ready, starting initial ranking sync");
        try {
            rankingSyncService.syncAllRankingsFromDatabase();
            log.info("[RankingScheduler] Initial ranking sync completed successfully");
        } catch (Exception e) {
            log.error("[RankingScheduler] Failed to perform initial ranking sync", e);
        }
    }

    /**
     * 매일 새벽 4시에 전체 동기화 수행
     * Redis 데이터의 정합성 보장 및 누락된 데이터 복구
     */
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(
        name = "RankingScheduler_fullSync",
        lockAtMostFor = "1h",
        lockAtLeastFor = "5m"
    )
    public void performFullSync() {
        log.info("[RankingScheduler] Starting scheduled full ranking sync");
        try {
            long startTime = System.currentTimeMillis();
            rankingSyncService.syncAllRankingsFromDatabase();
            long duration = System.currentTimeMillis() - startTime;
            log.info("[RankingScheduler] Full ranking sync completed in {} ms", duration);
        } catch (Exception e) {
            log.error("[RankingScheduler] Failed to perform full ranking sync", e);
        }
    }

    /**
     * 매 1시간마다 증분 동기화 수행
     * 최근 활동이 있는 유저들만 동기화하여 성능 최적화
     */
    @Scheduled(fixedDelay = 3600000, initialDelay = 600000) // 1시간마다, 초기 지연 10분
    @SchedulerLock(
        name = "RankingScheduler_incrementalSync",
        lockAtMostFor = "10m",
        lockAtLeastFor = "1m"
    )
    public void performIncrementalSync() {
        log.debug("[RankingScheduler] Starting scheduled incremental ranking sync");
        try {
            rankingSyncService.syncRecentlyActiveUsers();
            log.debug("[RankingScheduler] Incremental ranking sync completed");
        } catch (Exception e) {
            log.error("[RankingScheduler] Failed to perform incremental ranking sync", e);
        }
    }

    /**
     * 매주 일요일 새벽 3시에 Redis 데이터 정리 및 재구축
     * 오래된 데이터 제거 및 전체 재동기화
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    @SchedulerLock(
        name = "RankingScheduler_weeklyRebuild",
        lockAtMostFor = "2h",
        lockAtLeastFor = "10m"
    )
    public void performWeeklyRebuild() {
        log.info("[RankingScheduler] Starting weekly ranking rebuild");
        try {
            // 전체 재동기화 수행
            rankingSyncService.syncAllRankingsFromDatabase();
            log.info("[RankingScheduler] Weekly ranking rebuild completed successfully");
        } catch (Exception e) {
            log.error("[RankingScheduler] Failed to perform weekly ranking rebuild", e);
        }
    }
}
