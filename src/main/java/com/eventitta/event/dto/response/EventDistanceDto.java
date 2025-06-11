package com.eventitta.event.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record EventDistanceDto(
    Long id,
    String title,
    String place,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    LocalDateTime startTime,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    LocalDateTime endTime,

    String category,
    Boolean isFree,
    String homepageUrl,
    Double distance
) {
}
