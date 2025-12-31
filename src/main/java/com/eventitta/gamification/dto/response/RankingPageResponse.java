package com.eventitta.gamification.dto.response;

import com.eventitta.gamification.domain.RankingType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 순위 페이지 응답 DTO
 * 순위 목록과 전체 유저 수, 순위 타입 정보를 포함
 *
 * @param rankings 순위 목록
 * @param totalUsers 전체 유저 수
 * @param type 순위 타입 (POINTS, ACTIVITY_COUNT)
 */
@Schema(description = "순위 페이지 정보")
public record RankingPageResponse(
    @Schema(description = "순위 목록")
    List<UserRankResponse> rankings,

    @Schema(description = "전체 유저 수", example = "1000")
    long totalUsers,

    @Schema(description = "순위 타입", example = "POINTS")
    RankingType type
) {}
