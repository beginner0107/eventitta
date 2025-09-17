package com.eventitta.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "유저 랭킹 응답")
public record UserRankingResponse(
    @Schema(description = "닉네임")
    String nickname,
    @Schema(description = "총 획득 포인트")
    int totalPoints,
    @Schema(description = "마지막 활동 시각")
    LocalDateTime lastActivityAt
) {
}
