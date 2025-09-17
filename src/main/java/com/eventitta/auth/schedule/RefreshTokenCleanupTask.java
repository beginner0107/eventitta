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

    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "removeExpiredRefreshTokens", lockAtMostFor = "PT30M", lockAtLeastFor = "PT2M")
    @Transactional
    public void removeExpiredRefreshTokens() {
        try {
            long deleted = refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
            log.info("만료된 토큰을 삭제: {}", deleted);
        } catch (Exception e) {
            log.error("만료된 토큰 삭제 중 오류 발생", e);
        }
    }
}
