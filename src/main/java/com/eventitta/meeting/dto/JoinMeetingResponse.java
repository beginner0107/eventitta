package com.eventitta.meeting.dto;

import com.eventitta.meeting.domain.ParticipantStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임 참가 신청 응답")
public record JoinMeetingResponse(
    @Schema(description = "참가 신청 ID", example = "1")
    Long participantId,

    @Schema(description = "모임 ID", example = "1")
    Long meetingId,

    @Schema(description = "참가 상태", example = "PENDING")
    ParticipantStatus status,

    @Schema(description = "메시지", example = "모임 참가 신청이 완료되었습니다. 승인을 기다려주세요.")
    String message
) {
}
