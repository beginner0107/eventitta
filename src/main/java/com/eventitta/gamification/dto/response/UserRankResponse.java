package com.eventitta.gamification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 유저 순위 응답 DTO
 * 순위, 점수, 유저 정보를 포함
 *
 * @param userId 유저 ID
 * @param nickname 유저 닉네임
 * @param profilePictureUrl 프로필 이미지 URL
 * @param score 점수 (포인트 또는 활동 수)
 * @param rank 순위 (1부터 시작)
 */
@Schema(description = "유저 순위 정보")
public record UserRankResponse(
    @Schema(description = "유저 ID", example = "1")
    Long userId,

    @Schema(description = "유저 닉네임", example = "홍길동")
    String nickname,

    @Schema(description = "프로필 이미지 URL", example = "/uploads/profile/image.jpg", nullable = true)
    String profilePictureUrl,

    @Schema(description = "점수 (포인트 또는 활동 수)", example = "1500")
    int score,

    @Schema(description = "순위 (1부터 시작)", example = "1")
    long rank
) {}
