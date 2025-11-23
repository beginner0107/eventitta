package com.eventitta.gamification.domain;

import com.eventitta.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_activities")
public class UserActivity extends BaseTimeEntity {

    private static final Long SYSTEM_TARGET_ID = 0L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 50)
    private ResourceType resourceType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private int pointsEarned;

    public static UserActivity of(Long userId, ActivityType activityType, Long targetId) {
        return UserActivity.builder()
            .userId(userId)
            .activityType(activityType)
            .resourceType(activityType.getResourceType())
            .targetId(targetId)
            .pointsEarned(activityType.getDefaultPoint())
            .build();
    }

    public static UserActivity forSystem(Long userId, ActivityType activityType) {
        return of(userId, activityType, SYSTEM_TARGET_ID);
    }
}
