package com.eventitta.gamification.domain;

import com.eventitta.common.config.BaseTimeEntity;
import com.eventitta.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "user_activities",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "activity_type_id", "target_id"})
)
@AllArgsConstructor
@Builder
public class UserActivity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_type_id", nullable = false)
    private ActivityType activityType;

    @Column(nullable = false)
    private int pointsEarned;

    @Column(nullable = false)
    private Long targetId;

    public UserActivity(User user, ActivityType activityType, Long targetId) {
        this.user = user;
        this.activityType = activityType;
        this.pointsEarned = activityType.getDefaultPoint();
        this.targetId = targetId;
    }
}
