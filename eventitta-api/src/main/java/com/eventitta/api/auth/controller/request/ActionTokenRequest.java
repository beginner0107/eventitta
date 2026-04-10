package com.eventitta.api.auth.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "액션 토큰 확인 요청")
public record ActionTokenRequest(
    @Schema(description = "메일로 전달된 인증 토큰")
    @NotBlank
    String token
) {
}
