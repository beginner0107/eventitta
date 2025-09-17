package com.eventitta.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "대댓글 응답")
public record CommentChildDto(
    @Schema(description = "대댓글 ID", example = "1")
    Long id,
    @Schema(description = "대댓글 내용", example = "이 글 정말 좋아요!")
    String content,
    @Schema(description = "대댓글 작성자 닉네임", example = "개구리왕눈이")
    String nickname,
    @Schema(description = "대댓글 삭제여부", example = "false")
    boolean deleted,
    @Schema(description = "대댓글 작성일", example = "2023-01-01T00:00:00")
    LocalDateTime createdAt
) {
}
