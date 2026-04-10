package com.eventitta.domain.auth.dto;

import java.time.Instant;

public record ClientSessionMetadata(
    String maskedIp,
    String userAgent,
    Instant observedAt
) {
}
