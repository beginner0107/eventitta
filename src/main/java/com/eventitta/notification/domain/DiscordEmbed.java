package com.eventitta.notification.domain;

import lombok.Builder;

import java.util.List;

@Builder
public record DiscordEmbed(
    String title,
    String description,
    int color,
    List<DiscordField> fields,
    DiscordFooter footer
) {
}
