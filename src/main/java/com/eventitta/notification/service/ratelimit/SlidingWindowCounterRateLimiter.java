package com.eventitta.notification.service.ratelimit;

import com.eventitta.notification.domain.AlertLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class SlidingWindowCounterRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, AtomicInteger> alerts = new ConcurrentHashMap<>();
    private final Clock clock;

    private static final long BUCKET_SIZE_MILLIS = TimeUnit.MINUTES.toMillis(1);
    private static final int NUM_BUCKETS = 5;
    private static final long WINDOW_SIZE_MILLIS = NUM_BUCKETS * BUCKET_SIZE_MILLIS;

    @Override
    public boolean shouldSendAlert(String errorCode, AlertLevel level) {
        long now = clock.millis();
        long currentBucketStart = (now / BUCKET_SIZE_MILLIS) * BUCKET_SIZE_MILLIS;
        String currentKey = errorCode + ":" + level + ":" + currentBucketStart;

        int currentTotal = calculateTotalCountInWindow(errorCode, level, currentBucketStart);
        int limitCount = getMaxAlertsPerPeriod(level);

        if (currentTotal >= limitCount) {
            return false;
        }

        AtomicInteger count = alerts.computeIfAbsent(currentKey, k -> new AtomicInteger(0));
        count.incrementAndGet();
        return true;
    }

    private int calculateTotalCountInWindow(String errorCode, AlertLevel level, long currentBucketStart) {
        int totalCount = 0;
        for (int i = 0; i < NUM_BUCKETS; i++) {
            long bucketStart = currentBucketStart - (BUCKET_SIZE_MILLIS * i);
            String bucketKey = errorCode + ":" + level + ":" + bucketStart;

            AtomicInteger bucketCount = alerts.get(bucketKey);
            if (bucketCount != null) {
                totalCount += bucketCount.get();
            }
        }
        return totalCount;
    }

    @Scheduled(fixedRate = 300000)
    public void scheduledCleanup() {
        long now = clock.millis();
        long cutoffTime = now - (WINDOW_SIZE_MILLIS * 2);

        alerts.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            return isExpiredKey(key, cutoffTime);
        });
    }

    private boolean isExpiredKey(String key, long cutoffTime) {
        String[] parts = key.split(":");
        if (parts.length >= 3) {
            try {
                long bucketTimestamp = Long.parseLong(parts[2]);
                return bucketTimestamp < cutoffTime;
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return true;
    }

    @Override
    public void reset() {
        alerts.clear();
    }
}
