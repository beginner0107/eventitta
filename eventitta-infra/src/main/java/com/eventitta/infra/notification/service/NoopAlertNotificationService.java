package com.eventitta.infra.notification.service;

import com.eventitta.domain.notification.domain.AlertLevel;
import com.eventitta.domain.notification.service.AlertNotificationService;

public class NoopAlertNotificationService implements AlertNotificationService {

    @Override
    public void sendAlert(
        AlertLevel level,
        String errorCode,
        String message,
        String requestUri,
        String userInfo,
        Throwable exception
    ) {
    }
}
