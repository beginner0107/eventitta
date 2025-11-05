package com.eventitta.notification.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SlackField {
    String title;
    String value;
    @Builder.Default
    boolean isShort = true;
}
