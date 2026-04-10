package com.eventitta.domain.gamification.exception;

import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.common.exception.ErrorCode;

public class UserPointsException extends CustomException {
    public UserPointsException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public UserPointsException(ErrorCode errorCode) {
        super(errorCode);
    }
}
