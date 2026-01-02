package com.eventitta.notification.domain;

import lombok.Builder;

import java.util.List;

@Builder
public record DiscordMessage(
    String content,
    String username,
    List<DiscordEmbed> embeds
) {
}
