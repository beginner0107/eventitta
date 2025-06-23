package com.eventitta.gamification.domain;

public enum ActivityType {
    CREATE_POST(10, "게시글 작성"),
    CREATE_COMMENT(5, "댓글 작성"),
    LIKE_POST(1, "게시글 좋아요"),
    JOIN_MEETING(20, "모임 참여");

    private final int points;
    private final String description;

    ActivityType(int points, String description) {
        this.points = points;
        this.description = description;
    }

    public int getPoints() {
        return points;
    }

    public String getDescription() {
        return description;
    }
}
