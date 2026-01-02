package com.eventitta.gamification.scheduler;

import com.eventitta.gamification.service.RankingSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 순위 동기화 스케줄러
 * MySQL → Redis 동기화를 정기적으로 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private static final long INCREMENTAL_SYNC_INTERVAL_MS = 3_600_000L;    // 1시간
    private static final long INCREMENTAL_SYNC_INITIAL_DELAY_MS = 600_000L; // 10분

    private final RankingSyncService rankingSyncService;

    /**
     * 애플리케이션 시작 시 초기 동기화 수행
     * Redis가 비어있을 수 있으므로 전체 동기화 실행
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("[Scheduler] RankingScheduler - Application ready, starting initial ranking sync");
        try {
            rankingSyncService.syncAllRankingsFromDatabase();
            log.info("[Scheduler] RankingScheduler - Initial ranking sync completed successfully");
        } catch (Exception e) {
            log.error("[Scheduler] RankingScheduler - Failed to perform initial ranking sync", e);
        }
    }

    /**
     * 매일 새벽 4시에 전체 동기화 수행
     * Redis 데이터의 정합성 보장 및 누락된 데이터 복구
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @SchedulerLock(
        name = "RankingScheduler_fullSync",
        lockAtMostFor = "PT1H",
        lockAtLeastFor = "PT5M"
    )
    public void performFullSync() {
        log.info("[Scheduler] RankingScheduler - Starting scheduled full ranking sync");
        try {
            long startTime = System.currentTimeMillis();
            rankingSyncService.syncAllRankingsFromDatabase();
            long duration = System.currentTimeMillis() - startTime;
            log.info("[Scheduler] RankingScheduler - Full ranking sync completed in {} ms", duration);
        } catch (Exception e) {
            log.error("[Scheduler] RankingScheduler - Failed to perform full ranking sync", e);
        }
    }

    /**
     * 매 1시간마다 증분 동기화 수행
     * 최근 활동이 있는 유저들만 동기화하여 성능 최적화
     */
    @Scheduled(
        fixedDelay = INCREMENTAL_SYNC_INTERVAL_MS,
        initialDelay = INCREMENTAL_SYNC_INITIAL_DELAY_MS
    )
    @SchedulerLock(
        name = "RankingScheduler_incrementalSync",
        lockAtMostFor = "PT10M",
        lockAtLeastFor = "PT1M"
    )
    public void performIncrementalSync() {
        log.debug("[Scheduler] RankingScheduler - Starting scheduled incremental ranking sync");
        try {
            rankingSyncService.syncRecentlyActiveUsers();
            log.debug("[Scheduler] RankingScheduler - Incremental ranking sync completed");
        } catch (Exception e) {
            log.error("[Scheduler] RankingScheduler - Failed to perform incremental ranking sync", e);
        }
    }

    /**
     * 매주 일요일 새벽 2시에 Redis 데이터 정리 및 재구축
     * 오래된 데이터 제거 및 전체 재동기화
     */
    @Scheduled(cron = "0 0 2 * * SUN", zone = "Asia/Seoul")
    @SchedulerLock(
        name = "RankingScheduler_weeklyRebuild",
        lockAtMostFor = "PT2H",
        lockAtLeastFor = "PT10M"
    )
    public void performWeeklyRebuild() {
        log.info("[Scheduler] RankingScheduler - Starting weekly ranking rebuild");
        try {
            // 전체 재동기화 수행
            rankingSyncService.syncAllRankingsFromDatabase();
            log.info("[Scheduler] RankingScheduler - Weekly ranking rebuild completed successfully");
        } catch (Exception e) {
            log.error("[Scheduler] RankingScheduler - Failed to perform weekly ranking rebuild", e);
        }
    }
}
