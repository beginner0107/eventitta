package com.eventitta.infra.media.scheduler;

import com.eventitta.domain.media.service.MediaAssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "scheduler.image-cleanup.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class MediaAssetCleanupScheduler {

    private final MediaAssetService mediaAssetService;

    @Scheduled(cron = "0 0 4 * * SUN", zone = "Asia/Seoul")
    @SchedulerLock(name = "cleanupStaleMediaAssets", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    public void cleanupStaleAssets() {
        log.info("[Scheduler] stale media asset cleanup started");
        mediaAssetService.cleanupStaleAssets();
        log.info("[Scheduler] stale media asset cleanup finished");
    }
}
