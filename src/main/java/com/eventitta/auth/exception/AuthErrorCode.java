package com.eventitta.auth.exception;

import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    CONFLICTED_EMAIL("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    CONFLICTED_NICKNAME("이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),

    NOT_FOUND_USER_EMAIL("해당 이메일의 사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),

    ACCESS_TOKEN_INVALID("잘못된 액세스 토큰입니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_EXPIRED("액세스 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_MISSING("리프레시 토큰이 없습니다.", HttpStatus.BAD_REQUEST),
    REFRESH_TOKEN_INVALID("잘못된 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("리프레시 토큰이 만료되었습니다. 다시 로그인하세요.", HttpStatus.UNAUTHORIZED),

    INVALID_CREDENTIALS("이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),

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
    public AuthException defaultException() {
        return new AuthException(this);
    }

    @Override
    public AuthException defaultException(Throwable cause) {
        return new AuthException(this, cause);
    }
}
