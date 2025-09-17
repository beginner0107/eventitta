package com.eventitta.common.notification.service;

import com.eventitta.common.notification.domain.AlertLevel;

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
