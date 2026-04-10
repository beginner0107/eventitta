package com.eventitta.domain.gamification.repository;

import com.eventitta.domain.common.repository.BaseRepository;
import com.eventitta.domain.gamification.domain.ActivityType;
import com.eventitta.domain.gamification.domain.GamificationActionRecord;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface GamificationActionRecordRepository
    extends BaseRepository<GamificationActionRecord, Long>, GamificationActionRecordRepositoryCustom {

    long countByUserIdAndActivityType(Long userId, ActivityType activityTypeId);

    long deleteByUserIdAndActivityTypeAndTargetId(Long userId, ActivityType activityTypeId, Long targetId);

    void deleteByUserId(Long userId);
}
