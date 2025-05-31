package com.eventitta.comment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommentUpdateRequestDto(
    @NotBlank String content
) {
}
