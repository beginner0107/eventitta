package com.eventitta.gamification.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "badge_rules")
public class BadgeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "condition_json", columnDefinition = "TEXT", nullable = false)
    private String conditionJson;

    @Column(name = "description")
    private String description;

    @Column(nullable = false)
    private boolean enabled;

    @Builder
    public BadgeRule(Badge badge, String conditionJson, String description, boolean enabled) {
        this.badge = badge;
        this.conditionJson = conditionJson;
        this.description = description;
        this.enabled = enabled;
    }
}
