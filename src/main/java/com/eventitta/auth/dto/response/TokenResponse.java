package com.eventitta.auth.dto.response;

public record TokenResponse(String accessToken, String refreshToken) {
}
