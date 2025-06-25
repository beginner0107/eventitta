package com.eventitta.gamification.repository;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.BadgeRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BadgeRuleRepository extends JpaRepository<BadgeRule, Long> {
    List<BadgeRule> findByActivityType(ActivityType activityType);
}
