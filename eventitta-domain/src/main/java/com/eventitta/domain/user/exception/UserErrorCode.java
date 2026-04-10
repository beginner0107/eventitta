package com.eventitta.domain.user.exception;

import com.eventitta.domain.common.exception.ErrorCode;
import com.eventitta.domain.common.exception.ErrorStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    NOT_FOUND_USER_ID("사용자를 찾을 수 없습니다.", ErrorStatus.NOT_FOUND),
    CONFLICTED_EMAIL("이미 사용 중인 이메일입니다.", ErrorStatus.CONFLICT),
    INVALID_CURRENT_PASSWORD("현재 비밀번호가 올바르지 않습니다.", ErrorStatus.BAD_REQUEST),
    CONFLICTED_NICKNAME("이미 사용 중인 닉네임입니다.", ErrorStatus.CONFLICT),

    INVALID_POINTS_AMOUNT("포인트 금액은 양수여야 합니다.", ErrorStatus.BAD_REQUEST),
    INSUFFICIENT_POINTS("포인트가 부족합니다.", ErrorStatus.BAD_REQUEST),

    DEFAULT("예상치 못한 서버 오류가 발생했습니다", ErrorStatus.INTERNAL_SERVER_ERROR);

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
    public UserException defaultException() {
        return new UserException(this);
    }

    @Override
    public UserException defaultException(Throwable cause) {
        return new UserException(this, cause);
    }
}
