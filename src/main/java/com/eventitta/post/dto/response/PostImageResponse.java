package com.eventitta.post.dto.response;

import com.eventitta.post.domain.PostImage;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 이미지 정보")
public record PostImageResponse(
    @Schema(description = "이미지 엔티티 ID", example = "100")
    Long id,

    @Schema(description = "이미지 URL", example = "https://cdn.example.com/image.jpg")
    String imageUrl,

    @Schema(description = "이미지 정렬 순서", example = "1")
    int sortOrder
) {
    public static PostImageResponse from(PostImage img) {
        return new PostImageResponse(
            img.getId(),
            img.getImageUrl(),
            img.getSortOrder()
        );
    }
}
