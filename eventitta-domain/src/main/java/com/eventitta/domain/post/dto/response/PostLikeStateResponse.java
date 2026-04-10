package com.eventitta.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 좋아요 상태 응답")
public record PostLikeStateResponse(
    @Schema(description = "게시글 ID", example = "1")
    Long postId,
    @Schema(description = "현재 사용자의 좋아요 여부", example = "true")
    boolean likedByMe,
    @Schema(description = "좋아요 수", example = "10")
    int likeCount
) {
    public static PostLikeStateResponse liked(Long postId, int likeCount) {
        return new PostLikeStateResponse(postId, true, likeCount);
    }

    public static PostLikeStateResponse unliked(Long postId, int likeCount) {
        return new PostLikeStateResponse(postId, false, likeCount);
    }
}
