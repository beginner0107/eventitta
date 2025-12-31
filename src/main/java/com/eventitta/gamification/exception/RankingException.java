package com.eventitta.gamification.exception;

import com.eventitta.common.exception.CustomException;
import com.eventitta.common.exception.ErrorCode;

/**
 * 순위 시스템 관련 예외
 */
public class RankingException extends CustomException {

    public RankingException(ErrorCode errorCode) {
        super(errorCode);
    }

    public RankingException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
