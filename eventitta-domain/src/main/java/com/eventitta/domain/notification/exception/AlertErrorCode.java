package com.eventitta.domain.notification.exception;

import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.common.exception.ErrorCode;
import com.eventitta.domain.common.exception.ErrorStatus;

public enum AlertErrorCode implements ErrorCode {

    SLACK_NOTIFICATION_FAILED("Slack 알림 전송에 실패했습니다.", ErrorStatus.INTERNAL_SERVER_ERROR),
    SLACK_CONFIGURATION_INVALID("Slack 설정이 유효하지 않습니다.", ErrorStatus.INTERNAL_SERVER_ERROR),
    RATE_LIMIT_EXCEEDED("알림 전송 한도를 초과했습니다.", ErrorStatus.TOO_MANY_REQUESTS),
    MESSAGE_BUILD_FAILED("알림 메시지 구성에 실패했습니다.", ErrorStatus.INTERNAL_SERVER_ERROR),
    ALERT_LEVEL_RESOLUTION_FAILED("알림 레벨 분석에 실패했습니다.", ErrorStatus.INTERNAL_SERVER_ERROR),
    INVALID_NOTIFICATION_PARAMETERS("알림 파라미터가 유효하지 않습니다.", ErrorStatus.BAD_REQUEST);

    private final String message;
    private final ErrorStatus status;

    AlertErrorCode(String message, ErrorStatus status) {
        this.message = message;
        this.status = status;
    }

    @Override
    public String defaultMessage() {
        return message;
    }

    @Override
    public ErrorStatus defaultHttpStatus() {
        return status;
    }

    @Override
    public RuntimeException defaultException() {
        return new CustomException(this);
    }

    @Override
    public RuntimeException defaultException(Throwable cause) {
        return new CustomException(this, cause);
    }
}
