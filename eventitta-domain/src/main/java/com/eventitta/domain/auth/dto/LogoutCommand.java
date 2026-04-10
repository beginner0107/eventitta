package com.eventitta.domain.auth.dto;

public record LogoutCommand(
    String accessToken,
    String refreshToken
) {
}
