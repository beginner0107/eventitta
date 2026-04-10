package com.eventitta.domain.gamification.exception;

import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.common.exception.ErrorCode;

public class UserActivityException extends CustomException {
    public UserActivityException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public UserActivityException(ErrorCode errorCode) {
        super(errorCode);
    }
}
