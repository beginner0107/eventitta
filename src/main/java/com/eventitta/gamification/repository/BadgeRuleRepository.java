package com.eventitta.gamification.repository;

import com.eventitta.gamification.domain.BadgeRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BadgeRuleRepository extends JpaRepository<BadgeRule, Long> {

    @Query("SELECT br FROM BadgeRule br " +
        "JOIN FETCH br.badge " +
        "WHERE br.enabled = true")
    List<BadgeRule> findAllEnabledWithBadge();
}
