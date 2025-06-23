package com.eventitta.gamification.repository;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    long countByUserIdAndActivityType(Long userId, ActivityType activityType);

    Optional<UserActivity> findByUserIdAndActivityTypeAndTargetId(Long userId, ActivityType activityType, Long targetId);
}
