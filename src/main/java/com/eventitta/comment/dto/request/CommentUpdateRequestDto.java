package com.eventitta.comment.dto.request;

import jakarta.validation.constraints.NotBlank;

import static com.eventitta.common.constants.ValidationMessage.COMMENT_CONTENT;

public record CommentUpdateRequestDto(
    @NotBlank(message = COMMENT_CONTENT)
    String content
) {
}
