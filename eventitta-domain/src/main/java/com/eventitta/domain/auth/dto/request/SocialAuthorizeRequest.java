package com.eventitta.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "카카오 소셜 로그인 인가 URL 요청")
public record SocialAuthorizeRequest(
    @Schema(description = "인가 코드가 전달될 redirect URI")
    @NotBlank
    String redirectUri
) {
}
