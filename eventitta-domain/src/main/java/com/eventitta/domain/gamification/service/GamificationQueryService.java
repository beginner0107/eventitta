package com.eventitta.domain.gamification.service;

import com.eventitta.domain.gamification.api.internal.facade.GamificationQueryFacade;
import com.eventitta.domain.gamification.api.internal.view.ActivitySummaryView;
import com.eventitta.domain.gamification.repository.UserActivityStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GamificationQueryService implements GamificationQueryFacade {

    private final UserActivityStatsRepository userActivityStatsRepository;

    @Override
    public List<ActivitySummaryView> getActivitySummary(Long userId) {
        return userActivityStatsRepository.findByUserIdOrderByActionTypeAsc(userId).stream()
            .map(stats -> new ActivitySummaryView(
                stats.getActionType().name(),
                stats.getActionCount(),
                stats.getPointsTotal()
            ))
            .toList();
    }
}
