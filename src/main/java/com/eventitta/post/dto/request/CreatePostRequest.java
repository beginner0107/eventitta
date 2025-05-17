package com.eventitta.post.dto.request;

public record CreatePostRequest(
    String title,
    String content,
    String regionCode
) {
}
