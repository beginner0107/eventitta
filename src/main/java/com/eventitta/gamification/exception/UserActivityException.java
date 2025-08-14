package com.eventitta.gamification.exception;

import com.eventitta.common.exception.CustomException;
import com.eventitta.common.exception.ErrorCode;

public class UserActivityException extends CustomException {
    public UserActivityException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public UserActivityException(ErrorCode errorCode) {
        super(errorCode);
    }
}
