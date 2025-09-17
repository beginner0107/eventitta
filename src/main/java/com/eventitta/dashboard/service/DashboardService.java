package com.eventitta.dashboard.service;

import com.eventitta.dashboard.enums.RankingPeriod;
import com.eventitta.dashboard.dto.response.UserRankingResponse;
import com.eventitta.dashboard.repository.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    private final DashboardRepository dashboardRepository;

    public List<UserRankingResponse> getUserRankings(RankingPeriod period) {
        LocalDateTime from = null;
        if (period == RankingPeriod.WEEKLY) {
            from = LocalDateTime.now().minusDays(7);
        }
        return dashboardRepository.findTopRankings(from, 10);
    }
}
