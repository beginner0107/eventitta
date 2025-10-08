package com.eventitta.gamification.domain;

import lombok.Getter;

@Getter
public enum ResourceType {
    POST("게시글"),
    COMMENT("댓글"),
    MEETING("모임"),
    SYSTEM("시스템");

    private final String description;

    ResourceType(String description) {
        this.description = description;
    }
}
