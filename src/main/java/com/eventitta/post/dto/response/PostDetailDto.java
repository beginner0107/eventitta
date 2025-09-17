package com.eventitta.post.dto.response;

import com.eventitta.post.domain.Post;
import com.eventitta.post.domain.PostImage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Schema(description = "게시글 상세 정보 응답")
public record PostDetailDto(
    @Schema(description = "게시글 ID", example = "1")
    Long id,
    @Schema(description = "게시글 제목", example = "맛집 게시글 0")
    String title,
    @Schema(description = "게시글 내용", example = "이곳은 테스트 본문입니다.")
    String content,
    @Schema(description = "작성자 닉네임", example = "foo")
    String authorNickname,
    @Schema(description = "작성자 프로필 이미지 URL", example = "https://cdn.example.com/profile.jpg")
    String authorProfileUrl,
    @Schema(description = "작성자 ID", example = "1")
    Long authorId,
    @Schema(description = "지역 코드", example = "1100110100")
    String regionCode,
    @Schema(description = "추천 수", example = "10")
    int likeCount,
    @Schema(description = "댓글 수", example = "6")
    int commentCount,
    @Schema(description = "게시글에 포함된 이미지 리스트 (정렬 순서 오름차순)")
    List<PostImageDto> images,
    @Schema(description = "생성일시", example = "2025-05-24T14:20:52")
    LocalDateTime createdAt,
    @Schema(description = "수정일시", example = "2025-05-24T15:00:00")
    LocalDateTime updatedAt
) {
    public static PostDetailDto from(Post p, int commentCount) {
        List<PostImageDto> images = p.getImages().stream()
            .sorted(Comparator.comparingInt(PostImage::getSortOrder))
            .map(PostImageDto::from)
            .toList();

        return new PostDetailDto(
            p.getId(),
            p.getTitle(),
            p.getContent(),
            p.getUser().getNickname(),
            p.getUser().getProfilePictureUrl(),
            p.getUser().getId(),
            p.getRegion().getCode(),
            p.getLikeCount(),
            commentCount,
            images,
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }
}
