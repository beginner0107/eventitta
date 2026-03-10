package com.eventitta.auth.service.dto;

import com.eventitta.user.domain.User;

public record SignUpResult(
    String email,
    String nickname
) {
    public static SignUpResult of(User user) {
        return new SignUpResult(
            user.getEmail(),
            user.getNickname()
        );
    }
}
