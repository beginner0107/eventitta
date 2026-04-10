package com.eventitta.domain.gamification.exception;

import com.eventitta.domain.common.exception.ErrorCode;
import com.eventitta.domain.common.exception.ErrorStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UserPointsErrorCode implements ErrorCode {
    NOT_FOUND_USER_POINTS("사용자 포인트를 찾을 수 없습니다.", ErrorStatus.NOT_FOUND);

    private final String message;
    private final ErrorStatus status;

    @Override
    public String defaultMessage() {
        return message;
    }

    @Override
    public ErrorStatus defaultHttpStatus() {
        return status;
    }

    @Override
    public UserPointsException defaultException() {
        return new UserPointsException(this);
    }

    @Override
    public UserPointsException defaultException(Throwable cause) {
        return new UserPointsException(this, cause);
    }
}
