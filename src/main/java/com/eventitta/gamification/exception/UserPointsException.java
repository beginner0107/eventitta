package com.eventitta.gamification.exception;

import com.eventitta.common.exception.CustomException;
import com.eventitta.common.exception.ErrorCode;

public class UserPointsException extends CustomException {
    public UserPointsException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public UserPointsException(ErrorCode errorCode) {
        super(errorCode);
    }
}
