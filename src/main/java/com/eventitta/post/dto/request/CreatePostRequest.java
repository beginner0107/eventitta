package com.eventitta.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 생성 요청")
public record CreatePostRequest(
    @Schema(description = "게시글 제목", example = "동네 맛집 추천")
    String title,
    @Schema(description = "게시글 내용", example = "여기는 정말 맛있어요!")
    String content,
    @Schema(description = "지역 코드", example = "1100110100")
    String regionCode
) {
}
