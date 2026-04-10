package com.eventitta.domain.user.api.internal.command;

public record RegisterSocialUserCommand(
    String email,
    String preferredNickname,
    String profilePictureUrl,
    String provider,
    String providerId
) {
}
