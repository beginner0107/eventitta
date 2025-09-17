package com.eventitta.gamification.repository;

import com.eventitta.gamification.domain.BadgeRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRuleRepository extends JpaRepository<BadgeRule, Long> {
}
