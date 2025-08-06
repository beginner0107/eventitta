package com.eventitta.festivals.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static com.eventitta.common.constants.ValidationMessage.*;

@Schema(name = "NearbyFestivalsRequest", description = "반경 검색을 위한 요청 파라미터")
public record NearbyFestivalsRequest(

    @NotNull(message = LAT)
    @Schema(description = "검색 기준 위도", example = "37.5665", requiredMode = Schema.RequiredMode.REQUIRED)
    Double latitude,

    @NotNull(message = LNG)
    @Schema(description = "검색 기준 경도", example = "126.9780", requiredMode = Schema.RequiredMode.REQUIRED)
    Double longitude,

    @NotNull(message = DISTANCE)
    @DecimalMin(value = "0.1", message = DISTANCE_MIN)
    @Schema(description = "반경 거리 (단위: km, 최소 0.1)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    Double radius,

    @Schema(description = "검색 시작일 (yyyy-MM-dd)", example = "2025-06-10")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate periodStart,

    @Schema(description = "검색 종료일 (yyyy-MM-dd)", example = "2025-06-20")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate periodEnd,

    @Min(value = 0, message = PAGE_MIN)
    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    Integer page,

    @Min(value = 1, message = SIZE_MIN)
    @Max(value = 100, message = SIZE_MAX)
    @Schema(description = "페이지 크기 (1~100)", example = "20")
    Integer size

) {
    public NearbyFestivalsRequest {
        if (page == null || page < 0) page = 0;
        if (size == null || size < 1 || size > 100) size = 20;
    }

    public LocalDateTime getStartDateTime() {
        LocalDate start = (periodStart != null ? periodStart : LocalDate.now());
        return start.atStartOfDay();
    }

    public LocalDateTime getEndDateTime() {
        if (periodEnd != null) {
            return periodEnd.atTime(LocalTime.MAX);
        }
        return LocalDateTime.of(9999, 12, 31, 23, 59, 59);
    }

    // 기존 코드와의 호환성을 위한 getter 메서드들
    public Double getLat() {
        return latitude;
    }

    public Double getLng() {
        return longitude;
    }

    public Double getDistanceKm() {
        return radius;
    }

    public LocalDate getFrom() {
        return periodStart;
    }

    public LocalDate getTo() {
        return periodEnd;
    }
}
