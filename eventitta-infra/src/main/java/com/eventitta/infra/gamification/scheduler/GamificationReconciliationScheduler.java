package com.eventitta.infra.gamification.scheduler;

import com.eventitta.domain.gamification.service.GamificationReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "scheduler.gamification-reconciliation.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class GamificationReconciliationScheduler {

    private final GamificationReconciliationService gamificationReconciliationService;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @SchedulerLock(
        name = "GamificationReconciliationScheduler_rebuildRankings",
        lockAtMostFor = "PT1H",
        lockAtLeastFor = "PT1M"
    )
    public void reconcileGamificationProjections() {
        log.info("[GamificationReconciliation] Starting daily projection reconciliation");
        gamificationReconciliationService.rebuildRankings();
        gamificationReconciliationService.reconcileBadges();
    }
}
