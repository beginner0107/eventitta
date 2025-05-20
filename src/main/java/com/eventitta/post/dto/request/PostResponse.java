package com.eventitta.post.dto.request;

import com.eventitta.post.domain.Post;

import java.time.LocalDateTime;

public record PostResponse(
    Long id,
    String title,
    String content,
    String regionCode,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static PostResponse from(Post p) {
        return new PostResponse(
            p.getId(),
            p.getTitle(),
            p.getContent(),
            p.getRegion().getCode(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }
}
