package com.eventitta.domain.post.dto.response;

import com.eventitta.domain.media.api.internal.view.MediaDisplayView;
import com.eventitta.domain.media.domain.MediaProcessingStatus;
import com.eventitta.domain.post.domain.PostImage;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 이미지 정보")
public record PostImageResponse(
    @Schema(description = "이미지 엔티티 ID", example = "100")
    Long id,

    @Schema(description = "이미지 URL", example = "https://cdn.example.com/image.jpg")
    String imageUrl,
    @Schema(description = "썸네일 이미지 URL", example = "https://cdn.example.com/thumb.webp")
    String thumbnailUrl,
    @Schema(description = "표시 이미지 너비", example = "1280")
    Integer width,
    @Schema(description = "표시 이미지 높이", example = "720")
    Integer height,
    @Schema(description = "파생본 처리 상태", example = "READY")
    MediaProcessingStatus processingStatus,

    @Schema(description = "이미지 정렬 순서", example = "1")
    int sortOrder
) {
    public static PostImageResponse from(PostImage img, MediaDisplayView displayInfo) {
        return new PostImageResponse(
            img.getId(),
            displayInfo.imageUrl(),
            displayInfo.thumbnailUrl(),
            displayInfo.width(),
            displayInfo.height(),
            displayInfo.processingStatus(),
            img.getSortOrder()
        );
    }
}
