package com.eventitta.common.notification.service;

import com.eventitta.common.notification.domain.AlertLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.eventitta.common.notification.constants.AlertConstants.RATE_LIMIT_WINDOW_MINUTES;

@Component
@RequiredArgsConstructor
public class SlidingWindowLogRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, List<Long>> alerts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final long RATE_LIMIT_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(RATE_LIMIT_WINDOW_MINUTES);

    @Override
    public boolean shouldSendAlert(String errorCode, AlertLevel level) {
        String key = createKey(errorCode, level);

        List<Long> timestamps = alerts.computeIfAbsent(key, k -> new ArrayList<>());
        long now = clock.millis();
        timestamps.removeIf(ts -> ts <= now - RATE_LIMIT_WINDOW_MILLIS);
        int maxAlerts = getMaxAlertsPerPeriod(level);
        if (timestamps.size() < maxAlerts) {
            timestamps.add(now);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
        alerts.clear();
    }

}
