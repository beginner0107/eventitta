package com.eventitta.event.exception;

import com.eventitta.common.exception.CustomException;
import com.eventitta.common.exception.ErrorCode;

public class EventException extends CustomException {
    public EventException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EventException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
