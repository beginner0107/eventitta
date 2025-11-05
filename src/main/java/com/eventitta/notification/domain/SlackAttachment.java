package com.eventitta.notification.domain;

import lombok.Builder;

import java.util.List;

@Builder
public record SlackAttachment(
    String color,
    String title,
    String text,
    List<SlackField> fields,
    long ts) {
}
