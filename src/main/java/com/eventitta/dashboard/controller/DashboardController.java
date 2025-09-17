package com.eventitta.dashboard.controller;

import com.eventitta.dashboard.enums.RankingPeriod;
import com.eventitta.dashboard.dto.response.UserRankingResponse;
import com.eventitta.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Dashboard", description = "대시보드 통계 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "유저 랭킹 조회")
    @GetMapping("/rankings")
    public ResponseEntity<List<UserRankingResponse>> getRankings(
        @RequestParam(value = "period", required = false, defaultValue = "all") String period
    ) {
        RankingPeriod p = RankingPeriod.from(period);
        List<UserRankingResponse> list = dashboardService.getUserRankings(p);
        return ResponseEntity.ok(list);
    }
}
