package com.eventitta.auth.service.dto;

public record TokenResult(
    String accessToken,
    String refreshToken
) {
}
