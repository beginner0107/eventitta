package com.eventitta.meeting.exception;

import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum MeetingErrorCode implements ErrorCode {
    // Meeting 조회
    MEETING_NOT_FOUND("존재하지 않는 모임입니다.", HttpStatus.NOT_FOUND),
    ALREADY_DELETED_MEETING("이미 삭제된 모임입니다.", HttpStatus.BAD_REQUEST),
    MEETING_NOT_RECRUITING("모집 중인 모임이 아닙니다.", HttpStatus.BAD_REQUEST),

    // Meeting 권한
    NOT_MEETING_LEADER("모임장만 수행할 수 있습니다.", HttpStatus.FORBIDDEN),

    // Meeting 검증
    INVALID_MEETING_TIME("모임 종료 시간은 시작 시간 이후여야 합니다.", HttpStatus.BAD_REQUEST),
    TOO_SMALL_MAX_MEMBERS("최대 인원은 현재 참여 인원보다 작을 수 없습니다.", HttpStatus.BAD_REQUEST),
    MEETING_FULL("모임 정원이 초과되었습니다.", HttpStatus.BAD_REQUEST),  // ✨ 통합

    // Participant
    PARTICIPANT_NOT_FOUND("참가 신청 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PARTICIPANT_NOT_IN_MEETING("해당 참가자는 이 모임에 속하지 않습니다.", HttpStatus.BAD_REQUEST),
    ALREADY_JOINED_MEETING("이미 참가 신청한 모임입니다.", HttpStatus.BAD_REQUEST),
    INVALID_PARTICIPANT_STATUS("올바르지 않은 참가자 상태입니다.", HttpStatus.BAD_REQUEST),
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
