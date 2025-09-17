package com.eventitta.meeting.dto.response;

import com.eventitta.meeting.domain.ParticipantStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "모임 참여자 정보")
public record ParticipantResponse(
    @Schema(description = "참여자 ID", example = "1")
    Long id,

    @Schema(description = "사용자 ID", example = "1")
    Long userId,

    @Schema(description = "참여자 닉네임", example = "스프링러버")
    String nickname,

    @Schema(description = "참여자 프로필 이미지 URL", example = "https://example.com/profile.jpg")
    String profileUrl,

    @Schema(description = "참여 상태", example = "APPROVED")
    ParticipantStatus status
) {
}
