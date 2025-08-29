package com.eventitta.region.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "셀렉트 박스용 지역 옵션 DTO")
public record RegionOptionDto(
    @Schema(description = "지역 코드 (leaf)", example = "1100110100")
    String code,
    @Schema(description = "전체 지역 코드 경로(부모, 자식 순서)", example = "1100000000-1100100000-1100110100")
    String fullCode
) {
}
