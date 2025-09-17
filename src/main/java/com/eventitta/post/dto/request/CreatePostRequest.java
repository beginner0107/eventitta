package com.eventitta.post.dto.request;

import com.eventitta.common.constants.ValidationMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Schema(description = "게시글 생성 요청")
public record CreatePostRequest(
    @Schema(description = "게시글 제목", example = "동네 맛집 추천")
    @NotBlank(message = ValidationMessage.TITLE)
    String title,
    @Schema(description = "게시글 내용", example = "여기는 정말 맛있어요!")
    @NotBlank(message = ValidationMessage.CONTENT)
    String content,
    @Schema(description = "지역 코드", example = "1100110100")
    @NotBlank(message = ValidationMessage.REGION_CODE)
    String regionCode,
    @Schema(description = "이미지 URL 리스트", example = "[\"/uploads/aaa.png\",\"/uploads/bbb.jpg\"]")
    List<@NotBlank String> imageUrls
) {
}
