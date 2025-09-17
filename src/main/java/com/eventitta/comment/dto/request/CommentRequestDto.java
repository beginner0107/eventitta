package com.eventitta.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "댓글 작성 요청 DTO")
public record CommentRequestDto(

    @Schema(description = "댓글 내용", example = "이 글 정말 좋아요!", requiredMode = REQUIRED)
    @NotBlank(message = COMMENT_CONTENT)
    String content,

    @Schema(description = "부모 댓글 ID (대댓글일 경우에만 사용)", example = "1")
    Long parentCommentId
) {
    public static final String COMMENT_CONTENT = "댓글 내용을 입력해주세요.";
}
