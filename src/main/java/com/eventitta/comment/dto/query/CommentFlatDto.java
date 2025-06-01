package com.eventitta.comment.dto.query;

import java.time.LocalDateTime;

public record CommentFlatDto(
    Long id,
    String content,
    String nickname,
    boolean deleted,
    LocalDateTime createdAt,
    Long parentId
) {
}
