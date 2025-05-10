package com.eventitta.auth.dto.request;

public record SignInRequest(
    String email,
    String password
) {
}
