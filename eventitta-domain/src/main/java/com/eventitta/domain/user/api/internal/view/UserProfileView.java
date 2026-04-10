package com.eventitta.domain.user.api.internal.view;

public record UserProfileView(
    Long userId,
    String nickname,
    Long profilePictureMediaId,
    String profilePictureUrl,
    boolean deleted
) {
    public boolean isActive() {
        return !deleted;
    }
}
