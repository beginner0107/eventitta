package com.eventitta.common.notification.service;

import com.eventitta.common.notification.config.SlackProperties;
import com.eventitta.common.notification.domain.AlertLevel;
import com.eventitta.common.notification.domain.SlackMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class SlackNotificationService {

    private final SlackProperties slackProperties;
    private final RateLimiter rateLimiter;
    private final RestClient slackRestClient;
    private final Environment environment;
    private final SlackMessageBuilder messageBuilder;

    public SlackNotificationService(
        SlackProperties slackProperties,
        @Qualifier("cacheBasedRateLimiter") RateLimiter rateLimiter,
        @Qualifier("slackRestClient") RestClient slackRestClient,
        Environment environment,
        SlackMessageBuilder messageBuilder
    ) {
        this.slackProperties = slackProperties;
        this.rateLimiter = rateLimiter;
        this.slackRestClient = slackRestClient;
        this.environment = environment;
        this.messageBuilder = messageBuilder;
    }

    @Async
    public void sendAlert(AlertLevel level, String errorCode, String message,
                          String requestUri, String userInfo, Throwable exception) {

        if (!slackProperties.isEnabled()) {
            return;
        }

        if (!rateLimiter.shouldSendAlert(errorCode, level)) {
            return;
        }

        try {
            SlackMessage slackMessage = createSlackMessage(level, errorCode, message,
                requestUri, userInfo, exception);
            sendToSlack(slackMessage);
            log.info("Slack 알림 전송 완료: {} - {}", level, errorCode);
        } catch (Exception e) {
            log.error("Slack 알림 전송 중 오류 발생: {}", e.getClass().getSimpleName());
        }
    }

    private SlackMessage createSlackMessage(AlertLevel level, String errorCode, String message,
                                            String requestUri, String userInfo, Throwable exception) {
        return messageBuilder.buildMessage(
            level, errorCode, message, requestUri, userInfo, exception,
            getActiveProfile(), slackProperties.getChannel(), slackProperties.getUsername()
        );
    }

    private void sendToSlack(SlackMessage message) {
        slackRestClient.post()
            .uri(slackProperties.getWebhookUrl())
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
