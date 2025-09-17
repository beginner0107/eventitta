package com.eventitta.common.notification.domain;

import lombok.Builder;

import java.util.List;

@Builder
public record SlackMessage(
    String channel,
    String username,
    String text,
    List<SlackAttachment> attachments) {
}
