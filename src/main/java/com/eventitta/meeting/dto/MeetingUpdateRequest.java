package com.eventitta.meeting.dto;

import com.eventitta.meeting.domain.MeetingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "모임 수정 요청")
public record MeetingUpdateRequest(
    @Schema(description = "모임 제목", example = "함께하는 스프링 스터디")
    @NotBlank(message = "모임 제목은 필수입니다.")
    String title,

    @Schema(description = "모임 설명", example = "매주 주말에 만나 스프링 심화 내용을 공부합니다.")
    String description,

    @Schema(description = "모임 시작 시간", example = "2025-08-01T10:00:00", type = "string")
    @NotNull(message = "시작 시간은 필수입니다.")
    @Future(message = "시작 시간은 현재 시간 이후여야 합니다.")
    LocalDateTime startTime,

    @Schema(description = "모임 종료 시간", example = "2025-08-01T12:00:00", type = "string")
    @NotNull(message = "종료 시간은 필수입니다.")
    @Future(message = "종료 시간은 현재 시간 이후여야 합니다.")
    LocalDateTime endTime,

    @Schema(description = "최대 참여 인원", example = "10")
    @Min(value = 2, message = "최대 인원은 2명 이상이어야 합니다.")
    int maxMembers,

    @Schema(description = "모임 장소 주소", example = "서울특별시 강남구 테헤란로 212")
    String address,

    @Schema(description = "위도", example = "37.5017")
    Double latitude,

    @Schema(description = "경도", example = "127.0396")
    Double longitude,

    @Schema(description = "모임 상태", example = "CLOSED")
    @NotNull(message = "모임 상태는 필수입니다.")
    MeetingStatus status
) {
}
