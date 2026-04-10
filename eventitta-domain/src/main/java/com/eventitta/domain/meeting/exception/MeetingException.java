package com.eventitta.domain.meeting.exception;

import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.common.exception.ErrorCode;

public class MeetingException extends CustomException {

    public MeetingException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public MeetingException(ErrorCode errorCode) {
        super(errorCode);
    }
}
