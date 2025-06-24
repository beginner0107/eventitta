package com.eventitta.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 생성 응답")
public record CreatePostResponse(
    @Schema(description = "생성된 게시글의 ID", example = "123")
    Long id,
    @Schema(description = "획득한 배지명", example = "첫 게시글")
    String badgeName
) {
}
