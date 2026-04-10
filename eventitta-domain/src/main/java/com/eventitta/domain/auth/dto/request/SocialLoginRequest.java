package com.eventitta.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "카카오 소셜 로그인 요청")
public record SocialLoginRequest(
    @Schema(description = "카카오 인가 코드")
    @NotBlank
    String code,
    @Schema(description = "카카오 인가 요청 state")
    @NotBlank
    String state,
    @Schema(description = "인가 코드가 발급된 redirect URI")
    @NotBlank
    String redirectUri
) {
}
