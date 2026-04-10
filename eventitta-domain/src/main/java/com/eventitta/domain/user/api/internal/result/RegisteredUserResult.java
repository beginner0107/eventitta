package com.eventitta.domain.user.api.internal.result;

public record RegisteredUserResult(
    Long userId,
    String email,
    String nickname
) {
}
