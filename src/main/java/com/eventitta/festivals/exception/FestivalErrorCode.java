package com.eventitta.festivals.exception;

import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum FestivalErrorCode implements ErrorCode {
    NOT_FOUND_FESTIVAL("해당 축제를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_FESTIVAL_DATA("축제 데이터가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    EXTERNAL_API_ERROR("외부 API 호출 중 오류가 발생했습니다.", HttpStatus.SERVICE_UNAVAILABLE),
    DATA_SYNC_ERROR("데이터 동기화 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_LOCATION_RANGE("위치 검색 범위가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE("날짜 범위가 유효하지 않습니다.", HttpStatus.BAD_REQUEST);

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
    public FestivalException defaultException() {
        return new FestivalException(this);
    }

    @Override
    public FestivalException defaultException(Throwable cause) {
        return new FestivalException(this, cause);
    }
}
