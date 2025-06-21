package com.eventitta.meeting.dto;

import com.eventitta.common.constants.ValidationMessage;
import com.eventitta.meeting.domain.MeetingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;

import java.time.LocalDate;

@Schema(description = "모임 목록 조회를 위한 필터링 및 페이징 파라미터")
@ParameterObject
public record MeetingFilter(
    @Schema(description = "페이지 번호 (0부터 시작)", example = "0", defaultValue = "0")
    @Min(value = 0, message = ValidationMessage.PAGE_MIN)
    Integer page,

    @Schema(description = "페이지 크기", example = "10", defaultValue = "10")
    @Min(value = 1, message = ValidationMessage.SIZE_MIN)
    @Max(value = 100, message = ValidationMessage.SIZE_MAX)
    Integer size,

    @Schema(description = "지역명", example = "서울")
    String region,

    @Schema(description = "검색 키워드 (제목, 설명에서 검색)", example = "스터디")
    String keyword,

    @Schema(description = "모임 시작일 최소값 (yyyy-MM-dd)", example = "2025-06-21")
    LocalDate startDateFrom,

    @Schema(description = "모임 시작일 최대값 (yyyy-MM-dd)", example = "2025-12-31")
    LocalDate startDateTo,

    @Schema(description = "모임 상태", example = "RECRUITING")
    MeetingStatus status,

    @Schema(description = "현재 위치 기준 거리 필터링 (km)", example = "5.0")
    Double distance,

    @Schema(description = "위도 (거리 필터링 시 필요)", example = "37.5665")
    Double latitude,

    @Schema(description = "경도 (거리 필터링 시 필요)", example = "126.9780")
    Double longitude
) {
    public MeetingFilter {
        page = (page == null) ? 0 : page;
        size = (size == null) ? 10 : size;
    }
}
