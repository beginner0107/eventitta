package com.eventitta.domain.region.exception;

import com.eventitta.domain.common.exception.ErrorCode;
import com.eventitta.domain.common.exception.ErrorStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RegionErrorCode implements ErrorCode {
    REGION_NOT_FOUND("존재하지 않는 지역 코드입니다.", ErrorStatus.NOT_FOUND),
    NOT_FOUND_REGION_CODE("존재하지 않는 지역 코드입니다.", ErrorStatus.NOT_FOUND),
    INVALID_REGION_CODE("잘못된 지역 코드 형식입니다.", ErrorStatus.BAD_REQUEST),
    REGION_HIERARCHY_BROKEN("지역 계층 구조가 손상되었습니다. 관리자에게 문의하세요.", ErrorStatus.INTERNAL_SERVER_ERROR),
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
    public RegionException defaultException() {
        return new RegionException(this);
    }

    @Override
    public RegionException defaultException(Throwable cause) {
        return new RegionException(this, cause);
    }
}
