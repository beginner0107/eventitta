package com.eventitta.post.dto.response;

import com.eventitta.post.domain.Post;

import java.time.LocalDateTime;

public record PostResponse(
    Long id,
    String title,
    String content,
    String regionCode,
    String authorNickname,
    String authorProfileUrl,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static PostResponse from(Post p) {
        return new PostResponse(
            p.getId(),
            p.getTitle(),
            p.getContent(),
            p.getRegion().getCode(),
            p.getUser().getNickname(),
            p.getUser().getProfilePictureUrl(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }
}
