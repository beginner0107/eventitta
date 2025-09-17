package com.eventitta.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

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
    public static <U> PageResponse<U> of(Page<U> p) {
        return new PageResponse<>(
            p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages()
        );
    }

    public static <S, U> PageResponse<U> map(Page<S> p, Function<S, U> mapper) {
        return of(p.map(mapper));
    }
}
