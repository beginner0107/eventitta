package com.eventitta.event.exception;

import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum EventErrorCode implements ErrorCode {
    API_CALL_FAILED("외부 축제 API 호출 실패", HttpStatus.BAD_GATEWAY),
    SERVICE_KEY_MISSING("서비스 키가 비어 있습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_RESPONSE("API 응답이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY),
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
    public EventException defaultException() {
        return new EventException(this);
    }

    @Override
    public EventException defaultException(Throwable cause) {
        return new EventException(this, cause);
    }
}
