package com.eventitta.common.notification.service;

import com.eventitta.common.notification.domain.AlertLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class FixedWindowRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, AtomicInteger> alerts = new ConcurrentHashMap<>();
    private final Clock clock;

    private static final long WINDOW_SIZE_MILLIS = 60000; // 1분
    public static final long CLEANUP_LOOKBACK_MILLIS = WINDOW_SIZE_MILLIS * 2;

    @Override
    public boolean shouldSendAlert(String errorCode, AlertLevel level) {
        long now = clock.millis();
        long startWindowMillis = (now / WINDOW_SIZE_MILLIS) * WINDOW_SIZE_MILLIS;
        String key = errorCode + ":" + level + ":" + startWindowMillis;

        AtomicInteger count = alerts.computeIfAbsent(key, (k) -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();
        int maxAlertsPerPeriod = getMaxAlertsPerPeriod(level);

        return currentCount <= maxAlertsPerPeriod;
    }

    @Override
    public void reset() {
        alerts.clear();
    }

    @Scheduled(fixedRate = 300000)
    public void cleanUpAlerts() {
        long now = clock.millis();
        long cutoffTime = now - CLEANUP_LOOKBACK_MILLIS;

        alerts.entrySet().removeIf(entry -> isExpiredKey(entry.getKey(), cutoffTime));
    }

    private boolean isExpiredKey(String key, long cutoffTime) {
        String[] parts = key.split(":");
        if (parts.length >= 3) {
            try {
                long windowMillis = Long.parseLong(parts[2]);
                return windowMillis <= cutoffTime;
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return true;
    }
}
