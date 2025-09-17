package com.eventitta.region.exception;

import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum RegionErrorCode implements ErrorCode {
    NOT_FOUND_REGION_CODE("지역를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
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
    public RegionException defaultException() {
        return new RegionException(this);
    }

    @Override
    public RegionException defaultException(Throwable cause) {
        return new RegionException(this, cause);
    }
}
