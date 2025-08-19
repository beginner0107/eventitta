package com.eventitta.common.notification.exception;

import com.eventitta.common.exception.CustomException;

public class SlackNotificationException extends CustomException {

    public SlackNotificationException(AlertErrorCode errorCode) {
        super(errorCode);
    }

    public SlackNotificationException(AlertErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
