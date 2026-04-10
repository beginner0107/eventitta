package com.eventitta.domain.notification.service;

import com.eventitta.domain.notification.domain.AlertLevel;

public interface AlertNotificationService {

    void sendAlert(
        AlertLevel level,
        String errorCode,
        String message,
        String requestUri,
        String userInfo,
        Throwable exception
    );
}
