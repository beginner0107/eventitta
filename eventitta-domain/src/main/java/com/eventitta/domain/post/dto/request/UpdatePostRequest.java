package com.eventitta.domain.post.dto.request;

import com.eventitta.domain.common.constants.ValidationMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "게시글 수정 요청")
public record UpdatePostRequest(
    @Schema(description = "게시글 제목", example = "지금 시간이 남아서")
    @NotBlank(message = ValidationMessage.TITLE)
    String title,
    @Schema(description = "게시글 내용", example = "같이 드라이브하실 붐! 계신가용")
    @NotBlank(message = ValidationMessage.CONTENT)
    String content,
    @Schema(description = "지역 코드", example = "1100110100")
    @NotBlank(message = ValidationMessage.REGION_CODE)
    String regionCode,
    @Schema(description = "게시글 이미지 미디어 ID 리스트", example = "[101,102]")
    @Size(max = 5, message = "게시글 이미지는 최대 5개까지 첨부할 수 있습니다.")
    List<@NotNull Long> imageMediaIds
) {
}
