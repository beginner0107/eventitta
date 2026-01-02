package com.eventitta.notification.domain;

import lombok.Builder;

@Builder
public record DiscordFooter(
    String text
) {
}
