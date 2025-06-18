package com.eventitta.meeting.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record MeetingCreateRequest(
    @NotBlank(message = "모임 제목은 필수입니다.")
    String title,
    String description,
    @NotNull(message = "시작 시간은 필수입니다.")
    @Future(message = "시작 시간은 현재 시간 이후여야 합니다.")
    LocalDateTime startTime,
    @NotNull(message = "종료 시간은 필수입니다.")
    @Future(message = "종료 시간은 현재 시간 이후여야 합니다.")
    LocalDateTime endTime,
    @Min(value = 2, message = "최대 인원은 2명 이상이어야 합니다.")
    int maxMembers,
    String address,
    Double latitude,
    Double longitude
) {
}
