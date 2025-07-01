package com.eventitta.gamification.repository;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.dto.query.ActivitySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    long countByUserIdAndActivityType(Long userId, ActivityType activityType);

    Optional<UserActivity> findByUserIdAndActivityTypeAndTargetId(Long userId, ActivityType activityType, Long targetId);

    boolean existsByUserIdAndActivityTypeAndTargetId(Long userId, ActivityType activityType, Long targetId);

    @Query("SELECT ua.activityType AS activityType, COUNT(ua) AS count " +
        "FROM UserActivity ua " +
        "WHERE ua.user.id = :userId " +
        "GROUP BY ua.activityType")
    List<ActivitySummary> countActivitiesByUser(@Param("userId") Long userId);
}
