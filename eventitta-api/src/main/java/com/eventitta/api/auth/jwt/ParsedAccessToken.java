package com.eventitta.api.auth.jwt;

import java.time.Instant;

public record ParsedAccessToken(
    Long userId,
    String email,
    String role,
    long authVersion,
    String sessionId,
    Instant issuedAt,
    Instant expiresAt
) {
}
