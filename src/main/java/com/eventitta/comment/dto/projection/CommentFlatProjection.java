package com.eventitta.comment.dto.projection;

import java.time.LocalDateTime;

public record CommentFlatProjection(
    Long id,
    String content,
    String nickname,
    boolean deleted,
    LocalDateTime createdAt,
    Long parentId
) {
}
