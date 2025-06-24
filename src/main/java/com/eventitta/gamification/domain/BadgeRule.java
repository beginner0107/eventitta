package com.eventitta.gamification.domain;

import lombok.Getter;

@Getter
public enum BadgeRule {
    FIRST_POST(ActivityType.CREATE_POST, 1, "첫 게시글"),
    COMMENTER(ActivityType.CREATE_COMMENT, 10, "열혈 댓글러"),
    PRO_LIKER(ActivityType.LIKE_POST, 50, "프로 좋아요꾼"),
    FIRST_MEETING(ActivityType.JOIN_MEETING, 1, "첫 모임 참가");

    private final ActivityType activityType;
    private final long threshold;
    private final String badgeName;

    BadgeRule(ActivityType activityType, long threshold, String badgeName) {
        this.activityType = activityType;
        this.threshold = threshold;
        this.badgeName = badgeName;
    }
}
