package com.eventitta.domain.user.api.internal.view;

public record UserAuditView(
    Long userId,
    String email
) {
}
