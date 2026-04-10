package com.eventitta.domain.gamification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(UserActivityStatsId.class)
@Table(name = "user_activity_stats")
public class UserActivityStats {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private RewardActionType actionType;

    @Column(name = "action_count", nullable = false)
    private long actionCount;

    @Column(name = "points_total", nullable = false)
    private int pointsTotal;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
