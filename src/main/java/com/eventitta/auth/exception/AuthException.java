package com.eventitta.auth.exception;

import com.eventitta.common.exception.CustomException;
import com.eventitta.common.exception.ErrorCode;

public class AuthException extends CustomException {
    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
