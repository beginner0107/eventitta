package com.eventitta.domain.auth.service.dto;

public record SignInCommand(
    String email,
    String password,
    ClientSessionMetadata sessionMetadata
) {
    public SignInCommand(String email, String password) {
        this(email, password, null);
    }
}
