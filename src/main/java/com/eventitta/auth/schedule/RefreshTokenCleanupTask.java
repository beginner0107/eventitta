package com.eventitta.auth.schedule;

import com.eventitta.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupTask {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "removeExpiredRefreshTokens", lockAtMostFor = "PT30M", lockAtLeastFor = "PT2M")
    @Transactional
    public void removeExpiredRefreshTokens() {
        log.info("[Scheduler] 만료된 리프레시 토큰 정리 시작");

        try {
            long deleted = refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
            log.info("[Scheduler] 만료된 리프레시 토큰 정리 완료 - 삭제 건수: {}", deleted);
        } catch (Exception e) {
            log.error("[Scheduler] 만료된 리프레시 토큰 정리 실패 - error={}", e.getMessage(), e);
        }
    }
}
