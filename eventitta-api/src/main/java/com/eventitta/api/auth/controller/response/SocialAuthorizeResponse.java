package com.eventitta.api.auth.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카카오 인가 URL 응답")
public record SocialAuthorizeResponse(
    @Schema(description = "카카오 로그인 인가 URL")
    String authorizeUrl
) {
}
