package com.eventitta.domain.user.api.internal.view;

public record UserAuthView(
    Long userId,
    String email,
    String encodedPassword,
    String role,
    boolean emailVerified,
    boolean suspended,
    long authVersion,
    String nickname,
    String profilePictureUrl
) {
}
