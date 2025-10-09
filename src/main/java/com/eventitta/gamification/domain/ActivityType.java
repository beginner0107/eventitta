package com.eventitta.gamification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.eventitta.gamification.domain.ResourceType.*;

@Getter
@RequiredArgsConstructor
public enum ActivityType {
    CREATE_POST("게시글 작성", 10, POST),
    DELETE_POST("게시글 삭제", 10, POST),
    CREATE_COMMENT("댓글 작성", 5, COMMENT),
    DELETE_COMMENT("댓글 삭제", 5, COMMENT),
    LIKE_POST("게시글 좋아요", 1, POST),
    LIKE_POST_CANCEL("게시글 좋아요 취소", 1, POST),
    LIKE_COMMENT("댓글 좋아요", 1, COMMENT),
    LIKE_COMMENT_CANCEL("댓글 좋아요 취소", 1, COMMENT),
    JOIN_MEETING("모임 참여", 20, MEETING),
    JOIN_MEETING_CANCEL("모임 참여 취소", 20, MEETING),

    USER_LOGIN("회원 로그인", 5, SYSTEM);

    private final String displayName;
    private final int defaultPoint;
    private final ResourceType resourceType;

    public UserActivity createActivity(Long userId, Long targetId) {
        return switch (this.resourceType) {
            case POST -> UserActivity.forPost(userId, this, targetId);
            case COMMENT -> UserActivity.forComment(userId, this, targetId);
            case MEETING -> UserActivity.forMeeting(userId, this, targetId);
            case SYSTEM -> UserActivity.forSystem(userId, this);
            case UNKNOWN -> UserActivity.forUnknown(userId, this);
        };
    }
}
