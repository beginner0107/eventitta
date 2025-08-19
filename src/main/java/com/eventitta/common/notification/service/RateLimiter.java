package com.eventitta.common.notification.service;

import com.eventitta.common.notification.domain.AlertLevel;

public interface RateLimiter {
    boolean shouldSendAlert(String errorCode, AlertLevel level);
    void reset();
}
