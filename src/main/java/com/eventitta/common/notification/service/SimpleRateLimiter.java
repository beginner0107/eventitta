package com.eventitta.common.notification.service;

import com.eventitta.common.notification.constants.AlertConstants;
import com.eventitta.common.notification.domain.AlertLevel;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SimpleRateLimiter implements RateLimiter {

    private static final String KEY_SEPARATOR = ":";

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

        AlertRecord record = alerts.get(key);
        if (record == null) {
            alerts.put(key, new AlertRecord(now));
            return true;
        }

        int maxAlerts = getMaxAlertsPerPeriod(level);
        return record.incrementAndGet() <= maxAlerts;
    }

    @Override
    public void reset() {
        alerts.clear();
    }

    private String createKey(String errorCode, AlertLevel level) {
        return errorCode + KEY_SEPARATOR + level;
    }

    private void cleanupExpiredRecords(long now) {
        long windowMillis = TimeUnit.MINUTES.toMillis(AlertConstants.RATE_LIMIT_WINDOW_MINUTES);
        alerts.entrySet().removeIf(entry ->
            now - entry.getValue().getTimestamp() > windowMillis);
    }

    private int getMaxAlertsPerPeriod(AlertLevel level) {
        return level.getAlertLimit();
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
