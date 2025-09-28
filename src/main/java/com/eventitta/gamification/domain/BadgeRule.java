package com.eventitta.gamification.domain;

import com.eventitta.common.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "badge_rules")
public class BadgeRule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_type_id", nullable = false)
    private ActivityType activityType;

    @Column(nullable = false)
    private int threshold;

    @Column(nullable = false)
    private boolean enabled;

    @Builder
    public BadgeRule(Long id, Badge badge, ActivityType activityType, int threshold, boolean enabled) {
        this.id = id;
        this.badge = badge;
        this.activityType = activityType;
        this.threshold = threshold;
        this.enabled = enabled;
    }
}
