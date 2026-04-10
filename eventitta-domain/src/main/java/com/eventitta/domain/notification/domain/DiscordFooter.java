package com.eventitta.domain.notification.domain;

import lombok.Builder;

@Builder
public record DiscordFooter(
    String text
) {
}
