package com.eventitta.user.exception;

import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    NOT_FOUND_USER_ID("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_CURRENT_PASSWORD("현재 비밀번호가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    DEFAULT("예상치 못한 서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);

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
    public UserException defaultException() {
        return new UserException(this);
    }

    @Override
    public UserException defaultException(Throwable cause) {
        return new UserException(this, cause);
    }
}
