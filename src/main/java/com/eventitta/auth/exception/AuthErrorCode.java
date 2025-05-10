package com.eventitta.auth.exception;

import com.eventitta.common.exception.CustomException;
import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    CONFLICTED_EMAIL("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    CONFLICTED_NICKNAME("이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),

    NOT_FOUND_USER_EMAIL("해당 이메일의 사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
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
    public CustomException defaultException() {
        return new CustomException(this);
    }

    @Override
    public CustomException defaultException(Throwable cause) {
        return new CustomException(this, cause);
    }
}
