package com.eventitta.meeting.exception;

import com.eventitta.common.exception.CustomException;
import com.eventitta.common.exception.ErrorCode;

public class MeetingException extends CustomException {

    public MeetingException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public MeetingException(ErrorCode errorCode) {
        super(errorCode);
    }
}
