package com.eventitta.gamification.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // e.g., CREATE_COMMENT

    @Column(nullable = false)
    private String name; // e.g., 댓글 작성

    @Column(nullable = false)
    private int defaultPoint;

    @Builder
    public ActivityType(String code, String name, int defaultPoint) {
        this.code = code;
        this.name = name;
        this.defaultPoint = defaultPoint;
    }
}

