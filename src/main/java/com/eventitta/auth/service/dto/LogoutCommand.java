package com.eventitta.auth.service.dto;

public record LogoutCommand(
    String accessToken
) {
}
