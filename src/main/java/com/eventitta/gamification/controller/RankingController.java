package com.eventitta.gamification.controller;

import com.eventitta.auth.domain.UserPrincipal;
import com.eventitta.gamification.domain.RankingType;
import com.eventitta.gamification.dto.response.RankingPageResponse;
import com.eventitta.gamification.dto.response.UserRankResponse;
import com.eventitta.gamification.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 순위 시스템 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
@Validated
@Tag(name = "Rankings", description = "유저 순위 시스템 API")
public class RankingController {

    private final RankingService rankingService;

    /**
     * Top N 순위 조회
     * 포인트 또는 활동량 기준 상위 순위를 조회합니다
     *
     * @param type 순위 타입 (POINTS 또는 ACTIVITY_COUNT)
     * @param limit 조회할 순위 수 (1-500)
     * @return 순위 페이지 응답
     */
    @GetMapping("/top")
    @Operation(
        summary = "Top N 순위 조회",
        description = "포인트 또는 활동량 기준 상위 순위를 조회합니다"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "순위 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
        @ApiResponse(responseCode = "503", description = "Redis 연결 실패")
    })
    public ResponseEntity<RankingPageResponse> getTopRankings(
        @Parameter(description = "순위 타입", example = "POINTS")
        @RequestParam(defaultValue = "POINTS") RankingType type,

        @Parameter(description = "조회할 순위 수", example = "100")
        @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit
    ) {
        log.debug("[RankingAPI] Getting top {} rankings for type: {}", limit, type);
        RankingPageResponse response = rankingService.getTopRankings(type, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 순위 조회
     * 현재 로그인한 유저의 순위를 조회합니다
     *
     * @param type 순위 타입
     * @param userPrincipal 인증된 유저 정보
     * @return 유저 순위 응답
     */
    @GetMapping("/me")
    @Operation(
        summary = "내 순위 조회",
        description = "현재 로그인한 유저의 순위를 조회합니다"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "순위 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 요청"),
        @ApiResponse(responseCode = "404", description = "순위 정보를 찾을 수 없음"),
        @ApiResponse(responseCode = "503", description = "Redis 연결 실패")
    })
    public ResponseEntity<UserRankResponse> getMyRank(
        @Parameter(description = "순위 타입", example = "POINTS")
        @RequestParam(defaultValue = "POINTS") RankingType type,

        @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.debug("[RankingAPI] Getting rank for current user: userId={}, type={}", userId, type);

        UserRankResponse response = rankingService.getUserRank(type, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 유저 순위 조회
     * 지정된 유저의 순위 정보를 조회합니다
     *
     * @param userId 조회할 유저 ID
     * @param type 순위 타입
     * @return 유저 순위 응답
     */
    @GetMapping("/users/{userId}")
    @Operation(
        summary = "특정 유저 순위 조회",
        description = "지정된 유저의 순위 정보를 조회합니다"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "순위 조회 성공"),
        @ApiResponse(responseCode = "404", description = "유저 또는 순위 정보를 찾을 수 없음"),
        @ApiResponse(responseCode = "503", description = "Redis 연결 실패")
    })
    public ResponseEntity<UserRankResponse> getUserRank(
        @Parameter(description = "유저 ID", example = "1")
        @PathVariable Long userId,

        @Parameter(description = "순위 타입", example = "POINTS")
        @RequestParam(defaultValue = "POINTS") RankingType type
    ) {
        log.debug("[RankingAPI] Getting rank for user: userId={}, type={}", userId, type);
        UserRankResponse response = rankingService.getUserRank(type, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 순위 통계 조회
     * 각 순위 타입별 전체 참여자 수를 조회합니다
     *
     * @return 순위별 통계 정보
     */
    @GetMapping("/stats")
    @Operation(
        summary = "순위 통계 조회",
        description = "각 순위 타입별 전체 참여자 수를 조회합니다"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "통계 조회 성공"),
        @ApiResponse(responseCode = "503", description = "Redis 연결 실패")
    })
    public ResponseEntity<RankingStatsResponse> getRankingStats() {
        log.debug("[RankingAPI] Getting ranking statistics");

        Long pointsCount = rankingService.getTotalUsers(RankingType.POINTS);
        Long activityCount = rankingService.getTotalUsers(RankingType.ACTIVITY_COUNT);

        RankingStatsResponse stats = new RankingStatsResponse(
            pointsCount != null ? pointsCount : 0L,
            activityCount != null ? activityCount : 0L
        );

        return ResponseEntity.ok(stats);
    }

    /**
     * 순위 통계 응답 DTO
     */
    public record RankingStatsResponse(
        @Parameter(description = "포인트 순위 참여자 수")
        Long pointsRankingCount,

        @Parameter(description = "활동량 순위 참여자 수")
        Long activityRankingCount
    ) {}
}
