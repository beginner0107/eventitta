package com.eventitta.domain.post.dto.response;

import com.eventitta.domain.post.domain.Post;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "게시글 요약 정보 응답")
public record PostSummaryResponse(
    @Schema(description = "게시글 ID", example = "1")
    Long id,
    @Schema(description = "게시글 제목", example = "맛집 게시글 0")
    String title,
    @Schema(description = "작성자 닉네임", example = "foo")
    String authorNickname,
    @Schema(description = "지역 코드", example = "1100110100")
    String regionCode,
    @Schema(description = "추천 수", example = "10")
    int likeCount,
    @Schema(description = "현재 사용자의 좋아요 여부", example = "false")
    boolean likedByMe,
    @Schema(description = "생성일시", example = "2025-05-24T14:20:52")
    LocalDateTime createdAt
) {
    public static PostSummaryResponse from(Post p, String authorNickname, int likeCount, boolean likedByMe) {
        return new PostSummaryResponse(
            p.getId(),
            p.getTitle(),
            authorNickname,
            p.getRegionCode(),
            likeCount,
            likedByMe,
            p.getCreatedAt()
        );
    }
}
