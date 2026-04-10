package com.eventitta.domain.comment.dto.request;

import jakarta.validation.constraints.NotBlank;

import static com.eventitta.domain.common.constants.ValidationMessage.COMMENT_CONTENT;

public record UpdateCommentRequest(
    @NotBlank(message = COMMENT_CONTENT)
    String content
) {
}
