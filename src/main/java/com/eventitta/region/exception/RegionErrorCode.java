package com.eventitta.region.exception;

import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum RegionErrorCode implements ErrorCode {
    REGION_NOT_FOUND("존재하지 않는 지역 코드입니다.", HttpStatus.NOT_FOUND),
    NOT_FOUND_REGION_CODE("존재하지 않는 지역 코드입니다.", HttpStatus.NOT_FOUND), // 하위 호환성
    INVALID_REGION_CODE("잘못된 지역 코드 형식입니다.", HttpStatus.BAD_REQUEST),
    REGION_HIERARCHY_BROKEN("지역 계층 구조가 손상되었습니다. 관리자에게 문의하세요.", HttpStatus.INTERNAL_SERVER_ERROR),
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
