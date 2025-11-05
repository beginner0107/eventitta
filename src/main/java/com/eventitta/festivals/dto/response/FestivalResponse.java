package com.eventitta.festivals.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record FestivalResponse(
    @Schema(description = "행사 ID", example = "468")
    Long id,

    @Schema(description = "행사 제목", example = "2025 영양고추 Festival")
    String title,

    @Schema(description = "장소", example = "서울광장")
    String place,

    @Schema(description = "시작 시각(시:분)", example = "2025-06-27 00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    LocalDateTime startTime,

    @Schema(description = "종료 시각(시:분)", example = "2025-06-27 23:59")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    LocalDateTime endTime,

    @Schema(description = "카테고리", example = "축제")
    String category,

    @Schema(description = "무료 여부", example = "true")
    Boolean isFree,

    @Schema(description = "홈페이지 URL", example = "https://example.com/event/468")
    String homepageUrl,

    @Schema(description = "이용자와의 거리(km)", example = "0.25")
    Double distance
) {
}
