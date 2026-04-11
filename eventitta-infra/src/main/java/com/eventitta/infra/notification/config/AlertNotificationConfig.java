package com.eventitta.infra.notification.config;

import com.eventitta.domain.notification.service.AlertNotificationService;
import com.eventitta.infra.notification.service.DiscordAlertMessageBuilder;
import com.eventitta.infra.notification.service.DiscordAlertNotificationService;
import com.eventitta.infra.notification.service.NoopAlertNotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(DiscordAlertProperties.class)
public class AlertNotificationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "notification.discord", name = "enabled", havingValue = "true")
    public AlertNotificationService discordAlertNotificationService(
        DiscordAlertProperties properties,
        DiscordAlertMessageBuilder messageBuilder,
        Environment environment,
        Clock clock,
        RestClient.Builder restClientBuilder
    ) {
        return new DiscordAlertNotificationService(
            properties,
            messageBuilder,
            environment,
            clock,
            restClientBuilder
        );
    }

    @Bean
    @ConditionalOnMissingBean(AlertNotificationService.class)
    public AlertNotificationService noopAlertNotificationService() {
        return new NoopAlertNotificationService();
    }
}
