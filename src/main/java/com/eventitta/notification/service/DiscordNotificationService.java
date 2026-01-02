package com.eventitta.notification.service;

import com.eventitta.notification.properties.DiscordProperties;
import com.eventitta.notification.domain.AlertLevel;
import com.eventitta.notification.domain.DiscordMessage;
import com.eventitta.notification.service.ratelimit.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class DiscordNotificationService {

    private final DiscordProperties discordProperties;
    private final RateLimiter rateLimiter;
    private final RestClient discordRestClient;
    private final Environment environment;
    private final DiscordMessageBuilder messageBuilder;

    public DiscordNotificationService(
        DiscordProperties discordProperties,
        @Qualifier("cacheBasedRateLimiter") RateLimiter rateLimiter,
        @Qualifier("discordRestClient") RestClient discordRestClient,
        Environment environment,
        DiscordMessageBuilder messageBuilder
    ) {
        this.discordProperties = discordProperties;
        this.rateLimiter = rateLimiter;
        this.discordRestClient = discordRestClient;
        this.environment = environment;
        this.messageBuilder = messageBuilder;
    }

    @Async
    public void sendAlert(AlertLevel level, String errorCode, String message,
                          String requestUri, String userInfo, Throwable exception) {

        if (!discordProperties.isEnabled()) {
            return;
        }

        if (!rateLimiter.shouldSendAlert(errorCode, level)) {
            return;
        }

        try {
            DiscordMessage discordMessage = createDiscordMessage(level, errorCode, message,
                requestUri, userInfo, exception);
            sendToDiscord(discordMessage);
            log.info("[Discord 알림 전송 완료] level={}, errorCode={}", level, errorCode);
        } catch (Exception e) {
            log.error("[Discord 알림 전송 실패] errorType={}, message={}",
                e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private DiscordMessage createDiscordMessage(AlertLevel level, String errorCode, String message,
                                                String requestUri, String userInfo, Throwable exception) {
        return messageBuilder.buildMessage(
            level, errorCode, message, requestUri, userInfo, exception,
            getActiveProfile(), discordProperties.getUsername()
        );
    }

    private void sendToDiscord(DiscordMessage message) {
        discordRestClient.post()
            .uri(discordProperties.getWebhookUrl())
            .body(message)
            .retrieve()
            .toBodilessEntity();
    }

    private String getActiveProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            return activeProfiles[0];
        }
        return "default";
    }
}
