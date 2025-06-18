package com.eventitta.meeting.exception;

import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum MeetingErrorCode implements ErrorCode {
    MEETING_NOT_FOUND("존재하지 않는 모임입니다.", HttpStatus.NOT_FOUND),
    INVALID_MEETING_TIME("모임 종료 시간은 시작 시간 이후여야 합니다.", HttpStatus.BAD_REQUEST),
    ;

    private final String message;
    private final HttpStatus status;

    @Override
    public String defaultMessage() {
        return message;
    }

    @Override
    public HttpStatus defaultHttpStatus() {
        return status;
    }

    @Override
    public MeetingException defaultException() {
        return new MeetingException(this);
    }

    @Override
    public MeetingException defaultException(Throwable cause) {
        return new MeetingException(this, cause);
    }
}
