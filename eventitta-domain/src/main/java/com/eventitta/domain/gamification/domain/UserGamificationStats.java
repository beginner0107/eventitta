package com.eventitta.domain.gamification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_gamification_stats")
public class UserGamificationStats {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_points", nullable = false)
    private int totalPoints;

    @Column(name = "total_activity_count", nullable = false)
    private long totalActivityCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
