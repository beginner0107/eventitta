package com.eventitta.common.exception;


import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT("입력값이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_JSON("요청 본문이 JSON 형식이 아닙니다.", HttpStatus.BAD_REQUEST),
    INVALID_CONSTRAINT("검증 제약조건을 위반했습니다.", HttpStatus.BAD_REQUEST),
    MISSING_PARAMETER("필수 파라미터가 누락되었습니다.", HttpStatus.BAD_REQUEST),
    TYPE_MISMATCH("파라미터 타입이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),

    UNAUTHORIZED("인증이 필요합니다.", HttpStatus.UNAUTHORIZED),

    FORBIDDEN("해당 리소스에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    RESOURCE_NOT_FOUND("요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    METHOD_NOT_ALLOWED("지원하지 않는 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),

    DUPLICATE_RESOURCE("이미 존재하는 리소스입니다.", HttpStatus.CONFLICT),
    STATE_CONFLICT("현재 상태에서는 해당 요청을 수행할 수 없습니다.", HttpStatus.CONFLICT),
    LOCK_TIMEOUT("다른 사용자가 처리 중입니다. 잠시 후 다시 시도해주세요.", HttpStatus.CONFLICT),

    INTERNAL_ERROR("예상치 못한 서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("일시적인 오류로 요청을 처리할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE);

    private final String message;
    private final HttpStatus status;

    CommonErrorCode(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }

    @Override
    public String defaultMessage() {
        return message;
    }

    @Override
    public HttpStatus defaultHttpStatus() {
        return status;
    }

    @Override
    public RuntimeException defaultException() {
        return new CustomException(this);
    }

    @Override
    public RuntimeException defaultException(Throwable cause) {
        return new CustomException(this, cause);
    }
}
