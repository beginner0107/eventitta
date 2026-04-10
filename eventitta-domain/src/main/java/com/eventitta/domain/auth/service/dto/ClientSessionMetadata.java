package com.eventitta.domain.auth.service.dto;

import java.time.Instant;

public record ClientSessionMetadata(
    String maskedIp,
    String userAgent,
    Instant observedAt
) {
}
