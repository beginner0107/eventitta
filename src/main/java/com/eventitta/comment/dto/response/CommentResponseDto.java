package com.eventitta.comment.dto.response;


import com.eventitta.comment.domain.Comment;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponseDto(
    Long id,
    String content,
    String authorNickname,
    String authorProfileImage,
    LocalDateTime createdAt,
    List<CommentResponseDto> children
) {
    public static CommentResponseDto from(Comment comment) {
        return new CommentResponseDto(
            comment.getId(),
            comment.isDeleted() ? "[삭제된 댓글입니다]" : comment.getContent(),
            comment.getUser().getNickname(),
            comment.getUser().getProfilePictureUrl(),
            comment.getCreatedAt(),
            comment.getChildren().stream()
                .map(CommentResponseDto::from)
                .toList()
        );
    }
}
