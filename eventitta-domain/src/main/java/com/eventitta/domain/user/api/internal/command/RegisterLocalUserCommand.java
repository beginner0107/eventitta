package com.eventitta.domain.user.api.internal.command;

public record RegisterLocalUserCommand(
    String email,
    String encodedPassword,
    String nickname
) {
}
