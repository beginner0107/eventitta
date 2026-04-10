package com.eventitta.domain.gamification.api.internal.facade;

import com.eventitta.domain.gamification.api.internal.view.ActivitySummaryView;

import java.util.List;

public interface GamificationQueryFacade {

    List<ActivitySummaryView> getActivitySummary(Long userId);
}
