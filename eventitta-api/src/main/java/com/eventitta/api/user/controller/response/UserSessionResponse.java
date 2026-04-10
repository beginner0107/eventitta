package com.eventitta.api.user.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "사용자 세션 응답")
public record UserSessionResponse(
    @Schema(description = "세션 ID")
    String sessionId,
    @Schema(description = "세션 발급 시각")
    LocalDateTime issuedAt,
    @Schema(description = "마지막 활동 시각")
    LocalDateTime lastSeenAt,
    @Schema(description = "세션 만료 시각")
    LocalDateTime expiresAt,
    @Schema(description = "발급 시 IP 마스킹 정보")
    String issuedIpMasked,
    @Schema(description = "발급 시 User-Agent")
    String issuedUserAgent,
    @Schema(description = "마지막 활동 IP 마스킹 정보")
    String lastSeenIpMasked,
    @Schema(description = "마지막 활동 User-Agent")
    String lastSeenUserAgent,
    @Schema(description = "현재 세션 여부")
    boolean current
) {
}
