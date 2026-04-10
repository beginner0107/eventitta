package com.eventitta.domain.gamification.repository;

import com.eventitta.domain.common.repository.BaseRepository;
import com.eventitta.domain.gamification.domain.RewardActionType;
import com.eventitta.domain.gamification.domain.UserActivityStats;
import com.eventitta.domain.gamification.domain.UserActivityStatsId;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface UserActivityStatsRepository
    extends BaseRepository<UserActivityStats, UserActivityStatsId>, UserActivityStatsRepositoryCustom {

    List<UserActivityStats> findByUserIdOrderByActionTypeAsc(Long userId);

    Optional<UserActivityStats> findByUserIdAndActionType(Long userId, RewardActionType actionType);

    void deleteByUserId(Long userId);
}
