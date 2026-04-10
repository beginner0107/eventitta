package com.eventitta.domain.gamification.api.internal.facade;

public interface GamificationInternalFacade {

    void onPostCreated(Long userId, Long postId);

    void onPostDeleted(Long userId, Long postId);

    void onCommentCreated(Long userId, Long commentId);

    void onCommentDeleted(Long userId, Long commentId);

    void onMeetingJoinApproved(Long userId, Long meetingId);

    void onMeetingJoinCancelled(Long userId, Long meetingId);

    void removeUserData(Long userId);
}
