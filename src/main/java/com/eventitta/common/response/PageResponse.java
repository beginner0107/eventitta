package com.eventitta.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "페이지네이션된 응답 모델")
public record PageResponse<T>(

    @Schema(description = "조회된 컨텐츠 리스트")
    List<T> content,

    @Schema(description = "현재 페이지 번호", example = "0")
    int page,

    @Schema(description = "페이지 크기", example = "10")
    int size,

    @Schema(description = "전체 요소 개수", example = "123")
    long totalElements,

    @Schema(description = "전체 페이지 수", example = "13")
    int totalPages

) {
}
