package com.eventitta.gamification.repository;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.dto.projection.ActivitySummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    long countByUserIdAndActivityType(Long userId, ActivityType activityTypeId);

    @Query("SELECT ua.activityType AS activityType, " +
        "COUNT(ua) AS count, " +
        "SUM(ua.pointsEarned) AS totalPoints " +
        "FROM UserActivity ua " +
        "WHERE ua.userId = :userId " +
        "GROUP BY ua.activityType")
    List<ActivitySummaryProjection> countActivitiesByUser(@Param("userId") Long userId);

    long deleteByUserIdAndActivityTypeAndTargetId(Long userId, ActivityType activityTypeId, Long targetId);

    @Query("SELECT COUNT(ua) > 0 FROM UserActivity ua " +
        "WHERE ua.userId = :userId " +
        "AND ua.activityType = :activityType " +
        "AND ua.createdAt >= :startOfDay " +
        "AND ua.createdAt < :endOfDay")
    boolean existsTodayActivity(@Param("userId") Long userId,
                                @Param("activityType") ActivityType activityType,
                                @Param("startOfDay") LocalDateTime startOfDay,
                                @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 특정 유저의 전체 활동 수 조회
     */
    long countByUserId(Long userId);

    /**
     * 최근 N시간 이내 활동이 있는 유저 ID 목록 조회
     */
    @Query("SELECT DISTINCT ua.userId FROM UserActivity ua " +
        "WHERE ua.createdAt >= :since " +
        "ORDER BY ua.userId")
    List<Long> findRecentlyActiveUserIds(@Param("since") LocalDateTime since);

    /**
     * 최근 N시간 이내 활동이 있는 유저 ID 목록 조회
     */
    default List<Long> findRecentlyActiveUserIds(int hours) {
        return findRecentlyActiveUserIds(LocalDateTime.now().minusHours(hours));
    }

    /**
     * 여러 유저의 활동 수를 한 번에 조회
     *
     * @param userIds 조회할 유저 ID 목록
     * @return 유저 ID와 활동 수의 맵
     */
    @Query("SELECT ua.userId, COUNT(ua) FROM UserActivity ua " +
        "WHERE ua.userId IN :userIds " +
        "GROUP BY ua.userId")
    List<Object[]> countActivitiesByUserIds(@Param("userIds") List<Long> userIds);
}
