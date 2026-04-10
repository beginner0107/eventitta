package com.eventitta.domain.gamification.exception;

import com.eventitta.domain.common.exception.ErrorCode;
import com.eventitta.domain.common.exception.ErrorStatus;
import lombok.RequiredArgsConstructor;

/**
 * 순위 시스템 관련 에러 코드
 */
@RequiredArgsConstructor
public enum RankingErrorCode implements ErrorCode {
    RANKING_SYNC_FAILED("순위 동기화에 실패했습니다", ErrorStatus.INTERNAL_SERVER_ERROR),
    REDIS_CONNECTION_FAILED("Redis 연결에 실패했습니다", ErrorStatus.SERVICE_UNAVAILABLE),
    INVALID_RANKING_TYPE("유효하지 않은 순위 타입입니다", ErrorStatus.BAD_REQUEST),
    RANKING_NOT_FOUND("순위 정보를 찾을 수 없습니다", ErrorStatus.NOT_FOUND),
    RANKING_UPDATE_FAILED("순위 업데이트에 실패했습니다", ErrorStatus.INTERNAL_SERVER_ERROR);

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
    public RankingException defaultException() {
        return new RankingException(this);
    }

    @Override
    public RankingException defaultException(Throwable cause) {
        return new RankingException(this, cause);
    }
}
