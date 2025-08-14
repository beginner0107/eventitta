package com.eventitta.gamification.exception;

import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum UserActivityErrorCode implements ErrorCode {
    INVALID_ACTIVITY_TYPE("유효하지 않은 활동 코드입니다.", HttpStatus.BAD_REQUEST),
    DUPLICATED_USER_ACTIVITY("중복된 활동입니다.", HttpStatus.CONFLICT),
    CONCURRENT_MODIFICATION_RETRY_EXHAUSTED("동시 수정으로 인한 재시도 한도 초과", HttpStatus.CONFLICT);

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
