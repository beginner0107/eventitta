package com.eventitta.region.dto.response;

import com.eventitta.region.domain.Region;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지역 응답 DTO")
public record RegionResponse(
    @Schema(description = "지역 코드", example = "1100000000") String code,
    @Schema(description = "지역 이름", example = "서울특별시") String name,
    @Schema(description = "지역 레벨", example = "1") Integer level
) {
    public static RegionResponse from(Region region) {
        return new RegionResponse(region.getCode(), region.getName(), region.getLevel());
    }
}
