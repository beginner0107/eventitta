package com.eventitta.gamification.exception;

import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 순위 시스템 관련 에러 코드
 */
@RequiredArgsConstructor
public enum RankingErrorCode implements ErrorCode {
    RANKING_SYNC_FAILED("순위 동기화에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    REDIS_CONNECTION_FAILED("Redis 연결에 실패했습니다", HttpStatus.SERVICE_UNAVAILABLE),
    INVALID_RANKING_TYPE("유효하지 않은 순위 타입입니다", HttpStatus.BAD_REQUEST),
    RANKING_NOT_FOUND("순위 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    RANKING_UPDATE_FAILED("순위 업데이트에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR);

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
    public RankingException defaultException() {
        return new RankingException(this);
    }

    @Override
    public RankingException defaultException(Throwable cause) {
        return new RankingException(this, cause);
    }
}
