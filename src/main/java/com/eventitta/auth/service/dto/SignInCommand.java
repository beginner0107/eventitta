package com.eventitta.auth.service.dto;

public record SignInCommand(
    String email,
    String password
) {
}
