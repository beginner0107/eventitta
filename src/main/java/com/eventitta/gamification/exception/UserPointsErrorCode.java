package com.eventitta.gamification.exception;

import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum UserPointsErrorCode implements ErrorCode {
    NOT_FOUND_USER_POINTS("사용자 포인트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String message;
    private final HttpStatus status;

    @Override
    public String defaultMessage() {
        return message;
    }

    @Override
    public HttpStatus defaultHttpStatus() {
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
