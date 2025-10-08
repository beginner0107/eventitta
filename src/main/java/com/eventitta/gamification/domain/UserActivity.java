package com.eventitta.gamification.domain;

import com.eventitta.common.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.eventitta.gamification.domain.ResourceType.*;

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
    @Column(name = "target_type", nullable = false, length = 50)
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

    public static UserActivity forPost(Long userId, ActivityType activityType, Long postId) {
        return UserActivity.builder()
            .userId(userId)
            .activityType(activityType)
            .resourceType(POST)
            .targetId(postId)
            .pointsEarned(activityType.getDefaultPoint())
            .build();
    }

    public static UserActivity forComment(Long userId, ActivityType activityType, Long commentId) {
        return UserActivity.builder()
            .userId(userId)
            .activityType(activityType)
            .resourceType(COMMENT)
            .targetId(commentId)
            .pointsEarned(activityType.getDefaultPoint())
            .build();
    }

    public static UserActivity forMeeting(Long userId, ActivityType activityType, Long meetingId) {
        return UserActivity.builder()
            .userId(userId)
            .activityType(activityType)
            .resourceType(MEETING)
            .targetId(meetingId)
            .pointsEarned(activityType.getDefaultPoint())
            .build();
    }

    public static UserActivity forSystem(Long userId, ActivityType activityType) {
        return UserActivity.builder()
            .userId(userId)
            .activityType(activityType)
            .resourceType(SYSTEM)
            .targetId(SYSTEM_TARGET_ID)
            .pointsEarned(activityType.getDefaultPoint())
            .build();
    }
}
