package com.eventitta.auth.exception;

import com.eventitta.common.exception.ErrorCode;

public class AuthException extends RuntimeException {
    private final ErrorCode code;

    public AuthException(ErrorCode code) {
        super(code.defaultMessage());
        this.code = code;
    }

    public AuthException(ErrorCode code, Throwable cause) {
        super(code.defaultMessage(), cause);
        this.code = code;
    }

    public ErrorCode getErrorCode() {
        return code;
    }
}
