package com.eventitta.domain.auth.dto;

public record SignUpCommand(
    String email,
    String password,
    String nickname
) {
}
