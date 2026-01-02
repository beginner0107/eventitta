package com.eventitta.notification.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification.discord")
@Getter
@Setter
public class DiscordProperties {

    private static final String DEFAULT_USERNAME = "eventitta-bot";
    private static final int DEFAULT_TIMEOUT_SECONDS = 5;

    private boolean enabled = false;
    private String webhookUrl;
    private String username = DEFAULT_USERNAME;
    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
}
