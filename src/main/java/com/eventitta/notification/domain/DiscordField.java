package com.eventitta.notification.domain;

import lombok.Builder;

@Builder
public record DiscordField(
    String name,
    String value,
    boolean inline
) {
}
