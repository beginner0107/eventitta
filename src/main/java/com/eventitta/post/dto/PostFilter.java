package com.eventitta.post.dto;

import com.eventitta.common.constants.ValidationMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springdoc.core.annotations.ParameterObject;

@Schema(description = "게시글 목록 조회를 위한 필터링 및 페이징 파라미터")
@ParameterObject
public record PostFilter(

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0", minimum = "0")
    @Min(value = 0, message = ValidationMessage.PAGE_MIN)
    int page,

    @Schema(description = "페이지 크기", example = "10", minimum = "1", maximum = "100")
    @Min(value = 1, message = ValidationMessage.SIZE_MIN)
    @Max(value = 100, message = ValidationMessage.SIZE_MAX)
    int size,

    @Schema(description = "검색 타입 (TITLE, CONTENT, TITLE_CONTENT)", example = "TITLE_CONTENT", required = true)
    @NotNull(message = ValidationMessage.SEARCH_TYPE)
    SearchType searchType,

    @Schema(description = "검색 키워드", example = "맛집", required = true)
    @NotBlank(message = ValidationMessage.KEYWORD)
    String keyword,

    @Schema(description = "지역 코드", example = "1100110100", required = true)
    @NotBlank(message = ValidationMessage.REGION_CODE)
    String regionCode

) {
}
