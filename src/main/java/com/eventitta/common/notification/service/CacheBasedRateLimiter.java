package com.eventitta.common.notification.service;

import com.eventitta.common.notification.constants.AlertConstants;
import com.eventitta.common.notification.domain.AlertLevel;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CacheBasedRateLimiter implements RateLimiter {

    private static final String KEY_SEPARATOR = ":";

    private final Cache<String, AtomicInteger> alertCounts;

    public CacheBasedRateLimiter() {
        this.alertCounts = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(AlertConstants.RATE_LIMIT_WINDOW_MINUTES, TimeUnit.MINUTES)
            .recordStats()
            .build();
    }

    @Override
    public boolean shouldSendAlert(String errorCode, AlertLevel level) {
        String key = createKey(errorCode, level);

        AtomicInteger count = alertCounts.get(key, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();

        return currentCount <= getMaxAlertsPerPeriod(level);
    }

    @Override
    public void reset() {
        alertCounts.invalidateAll();
    }

    private String createKey(String errorCode, AlertLevel level) {
        return errorCode + KEY_SEPARATOR + level;
    }

    private int getMaxAlertsPerPeriod(AlertLevel level) {
        return level.getAlertLimit();
    }

    public CacheStats getCacheStats() {
        return alertCounts.stats();
    }

    public long getCacheSize() {
        return alertCounts.estimatedSize();
    }

    public void cleanUp() {
        alertCounts.cleanUp();
    }
}
