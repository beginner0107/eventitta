package com.eventitta.domain.gamification.repository;

import com.eventitta.domain.common.repository.BaseRepository;
import com.eventitta.domain.gamification.domain.ActivityType;
import com.eventitta.domain.gamification.domain.BadgeRule;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;

@NoRepositoryBean
public interface BadgeRuleRepository extends BaseRepository<BadgeRule, Long> {

    @Query("SELECT br FROM BadgeRule br " +
        "JOIN FETCH br.badge " +
        "WHERE br.enabled = true")
    List<BadgeRule> findAllEnabledWithBadge();

    @Query("SELECT br FROM BadgeRule br " +
        "JOIN FETCH br.badge " +
        "WHERE br.enabled = true AND br.activityType = :activityType")
    List<BadgeRule> findEnabledWithBadgeByActivityType(@Param("activityType") ActivityType activityType);
}
