package com.eventitta.domain.gamification.domain;

import com.eventitta.domain.common.domain.BaseTimeEntity;
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
@Table(
    name = "user_activities",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "activity_type", "target_id"})
)
public class GamificationActionRecord extends BaseTimeEntity {

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

    public static GamificationActionRecord grant(Long userId, ActivityType activityType, Long targetId) {
        return GamificationActionRecord.builder()
            .userId(userId)
            .activityType(activityType)
            .resourceType(activityType.getResourceType())
            .targetId(targetId)
            .pointsEarned(activityType.getDefaultPoint())
            .build();
    }
}
