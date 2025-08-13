package com.eventitta.gamification.exception;

import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum UserActivityErrorCode implements ErrorCode {
    DUPLICATED_USER_ACTIVITY("중복된 활동입니다.", HttpStatus.CONFLICT);

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
    public UserActivityException defaultException() {
        return new UserActivityException(this);
    }

    @Override
    public UserActivityException defaultException(Throwable cause) {
        return new UserActivityException(this, cause);
    }
}
