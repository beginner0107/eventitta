package com.eventitta.domain.user.exception;

import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.common.exception.ErrorCode;

public class UserException extends CustomException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UserException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
