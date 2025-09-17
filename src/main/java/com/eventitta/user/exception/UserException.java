package com.eventitta.user.exception;

import com.eventitta.common.exception.CustomException;
import com.eventitta.common.exception.ErrorCode;

public class UserException extends CustomException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UserException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
