package com.eventitta.domain.festivals.exception;

import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.common.exception.ErrorCode;

public class FestivalException extends CustomException {
    public FestivalException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FestivalException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
