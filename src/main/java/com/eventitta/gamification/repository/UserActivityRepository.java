package com.eventitta.gamification.repository;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.dto.query.ActivitySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    long countByUserIdAndActivityType(Long userId, ActivityType activityTypeId);

    @Query("SELECT ua.activityType AS activityType, COUNT(ua) AS count " +
        "FROM UserActivity ua " +
        "WHERE ua.userId = :userId " +
        "GROUP BY ua.activityType")
    List<ActivitySummary> countActivitiesByUser(@Param("userId") Long userId);

    long deleteByUserIdAndActivityTypeAndTargetId(Long userId, ActivityType activityTypeId, Long targetId);

    // 오늘 특정 활동이 이미 기록되었는지 확인
    @Query("SELECT COUNT(ua) > 0 FROM UserActivity ua " +
        "WHERE ua.userId = :userId " +
        "AND ua.activityType = :activityType " +
        "AND ua.createdAt >= :startOfDay " +
        "AND ua.createdAt < :endOfDay")
    boolean existsTodayActivity(@Param("userId") Long userId,
                                @Param("activityType") ActivityType activityType,
                                @Param("startOfDay") LocalDateTime startOfDay,
                                @Param("endOfDay") LocalDateTime endOfDay);
}
