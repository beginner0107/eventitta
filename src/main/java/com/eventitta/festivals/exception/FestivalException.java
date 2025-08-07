package com.eventitta.festivals.exception;

import com.eventitta.common.exception.CustomException;
import com.eventitta.common.exception.ErrorCode;

public class FestivalException extends CustomException {
    public FestivalException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FestivalException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
