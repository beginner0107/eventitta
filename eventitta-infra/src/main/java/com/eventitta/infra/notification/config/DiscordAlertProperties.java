package com.eventitta.infra.notification.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "notification.discord")
@Getter
@Setter
public class DiscordAlertProperties {

    private boolean enabled = false;
    private String webhookUrl;
    private String username = "eventitta-bot";

    @Min(1)
    private int timeoutSeconds = 5;

    @AssertTrue(message = "notification.discord.webhook-url must be configured when Discord alerts are enabled")
    public boolean isWebhookConfiguredWhenEnabled() {
        return !enabled || StringUtils.hasText(webhookUrl);
    }
}
