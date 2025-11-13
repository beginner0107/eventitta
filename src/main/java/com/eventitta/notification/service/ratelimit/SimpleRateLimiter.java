package com.eventitta.notification.service.ratelimit;

import com.eventitta.notification.constants.AlertConstants;
import com.eventitta.notification.domain.AlertLevel;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SimpleRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, AlertRecord> alerts = new ConcurrentHashMap<>();
    private final Clock clock;

    public SimpleRateLimiter() {
        this(Clock.systemDefaultZone());
    }

    public SimpleRateLimiter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean shouldSendAlert(String errorCode, AlertLevel level) {
        String key = createKey(errorCode, level);
        long now = clock.millis();

        cleanupExpiredRecords(now);

        final int maxAlerts = getMaxAlertsPerPeriod(level);
        AtomicBoolean allowed = new AtomicBoolean(false);

        alerts.compute(key, (k, record) -> {
            if (record == null) {
                // 첫 요청: 레코드 생성 및 허용
                allowed.set(true);
                return new AlertRecord(now);
            }
            int newCount = record.incrementAndGet();
            allowed.set(newCount <= maxAlerts);
            return record;
        });

        return allowed.get();
    }

    @Override
    public void reset() {
        alerts.clear();
    }

    private void cleanupExpiredRecords(long now) {
        long windowMillis = TimeUnit.MINUTES.toMillis(AlertConstants.RATE_LIMIT_WINDOW_MINUTES);
        alerts.entrySet().removeIf(entry ->
            now - entry.getValue().getTimestamp() > windowMillis);
    }

    private static class AlertRecord {
        private final long timestamp;
        private final AtomicInteger count;

        AlertRecord(long timestamp) {
            this.timestamp = timestamp;
            this.count = new AtomicInteger(1);
        }

        long getTimestamp() {
            return timestamp;
        }

        int incrementAndGet() {
            return count.incrementAndGet();
        }
    }
}
