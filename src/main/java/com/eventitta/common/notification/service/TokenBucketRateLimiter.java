package com.eventitta.common.notification.service;

import com.eventitta.common.notification.domain.AlertLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class TokenBucketRateLimiter implements RateLimiter {
    private final ConcurrentHashMap<String, TokenBucket> alerts = new ConcurrentHashMap<>();
    private final Clock clock;

    @Override
    public boolean shouldSendAlert(String errorCode, AlertLevel level) {
        long currentMillis = clock.millis();
        String key = createKey(errorCode, level);
        int capacity = getMaxAlertsPerPeriod(level);

        TokenBucket tokenBucket = alerts.computeIfAbsent(key, k -> new TokenBucket(currentMillis, capacity));

        synchronized (tokenBucket) {
            double refillRate = (double) capacity / TimeUnit.MINUTES.toMillis(5);
            long newTokens = (long) ((currentMillis - tokenBucket.timestamp) * refillRate);

            if (newTokens > 0) {
                tokenBucket.tokenCount = Math.min(capacity, tokenBucket.tokenCount + newTokens);
                tokenBucket.timestamp = currentMillis;
            }

            if (tokenBucket.tokenCount >= 1) {
                tokenBucket.tokenCount--;
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void reset() {
        alerts.clear();
    }

    private static class TokenBucket {
        long timestamp;
        long tokenCount;

        public TokenBucket(long timestamp, long tokenCount) {
            this.timestamp = timestamp;
            this.tokenCount = tokenCount;
        }
    }
}
