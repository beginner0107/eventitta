package com.eventitta.domain.auth.exception;

import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.common.exception.ErrorCode;

public class AuthException extends CustomException {
    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
