package com.eventitta.meeting.dto.response;

import com.eventitta.meeting.domain.ParticipantStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import static com.eventitta.meeting.constants.MeetingConstants.JOIN_MEETING_PENDING_MESSAGE;

@Schema(description = "모임 참가 신청 응답")
public record JoinMeetingResponse(
    @Schema(description = "참가 신청 ID", example = "1")
    Long participantId,

    @Schema(description = "모임 ID", example = "1")
    Long meetingId,

    @Schema(description = "참가 상태", example = "PENDING")
    ParticipantStatus status,

    @Schema(description = "메시지", example = JOIN_MEETING_PENDING_MESSAGE)
    String message
) {
}
