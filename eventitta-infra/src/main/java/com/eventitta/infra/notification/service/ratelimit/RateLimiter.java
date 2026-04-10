package com.eventitta.infra.notification.service.ratelimit;

import com.eventitta.domain.notification.domain.AlertLevel;

public interface RateLimiter {
    boolean shouldSendAlert(String errorCode, AlertLevel level);

    void reset();

    default String createKey(String errorCode, AlertLevel level) {
        return errorCode + ":" + level;
    }

    default int getMaxAlertsPerPeriod(AlertLevel level) {
        return level.getAlertLimit();
    }
}
