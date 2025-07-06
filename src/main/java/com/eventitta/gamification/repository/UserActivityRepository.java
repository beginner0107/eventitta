package com.eventitta.gamification.repository;

import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.dto.query.ActivitySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    Optional<UserActivity> findByUserIdAndActivityType_IdAndTargetId(Long userId, Long activityTypeId, Long targetId);

    boolean existsByUserIdAndActivityType_IdAndTargetId(Long userId, Long activityTypeId, Long targetId);

    long countByUserIdAndActivityType_Id(Long userId, Long activityTypeId);
    
    @Query("SELECT ua.activityType AS activityType, COUNT(ua) AS count " +
        "FROM UserActivity ua " +
        "WHERE ua.user.id = :userId " +
        "GROUP BY ua.activityType")
    List<ActivitySummary> countActivitiesByUser(@Param("userId") Long userId);
}
