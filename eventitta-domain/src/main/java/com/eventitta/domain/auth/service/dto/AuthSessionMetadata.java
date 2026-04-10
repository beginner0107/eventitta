package com.eventitta.domain.auth.service.dto;

import java.time.LocalDateTime;

public record AuthSessionMetadata(
    String sessionId,
    LocalDateTime issuedAt,
    LocalDateTime lastSeenAt,
    LocalDateTime expiresAt,
    String issuedIpMasked,
    String issuedUserAgent,
    String lastSeenIpMasked,
    String lastSeenUserAgent,
    boolean current
) {
}
