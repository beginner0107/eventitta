package com.eventitta.infra.notification.service;

import com.eventitta.domain.notification.constants.AlertConstants;
import com.eventitta.domain.notification.domain.AlertLevel;
import com.eventitta.domain.notification.domain.DiscordMessage;
import com.eventitta.domain.notification.service.AlertNotificationService;
import com.eventitta.infra.notification.config.DiscordAlertProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DiscordAlertNotificationService implements AlertNotificationService {

    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(AlertConstants.RATE_LIMIT_WINDOW_MINUTES);

    private final DiscordAlertProperties properties;
    private final DiscordAlertMessageBuilder messageBuilder;
    private final Environment environment;
    private final Clock clock;
    private final RestClient restClient;
    private final Map<String, AlertWindow> alertWindows = new ConcurrentHashMap<>();

    public DiscordAlertNotificationService(
        DiscordAlertProperties properties,
        DiscordAlertMessageBuilder messageBuilder,
        Environment environment,
        Clock clock,
        RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.messageBuilder = messageBuilder;
        this.environment = environment;
        this.clock = clock;
        this.restClient = restClientBuilder
            .requestFactory(createRequestFactory(properties))
            .build();
    }

    @Async
    @Override
    public void sendAlert(
        AlertLevel level,
        String errorCode,
        String message,
        String requestUri,
        String userInfo,
        Throwable exception
    ) {
        if (!shouldSend(errorCode, level)) {
            return;
        }

        try {
            DiscordMessage discordMessage = messageBuilder.build(
                level,
                errorCode,
                message,
                requestUri,
                userInfo,
                exception,
                activeProfile(),
                properties.getUsername()
            );

            restClient.post()
                .uri(properties.getWebhookUrl())
                .body(discordMessage)
                .retrieve()
                .toBodilessEntity();

            log.info("Discord alert sent. level={}, errorCode={}", level, errorCode);
        } catch (Exception sendException) {
            log.error("Discord alert send failed. errorCode={}, type={}", errorCode,
                sendException.getClass().getSimpleName(), sendException);
        }
    }

    private boolean shouldSend(String errorCode, AlertLevel level) {
        Instant now = clock.instant();
        cleanupExpired(now);

        String key = errorCode + ":" + level;
        AlertWindow window = alertWindows.compute(key, (ignored, current) -> {
            if (current == null || current.startedAt().plus(RATE_LIMIT_WINDOW).isBefore(now)) {
                return new AlertWindow(now, 1);
            }
            return new AlertWindow(current.startedAt(), current.count() + 1);
        });

        return window.count() <= level.getAlertLimit();
    }

    private void cleanupExpired(Instant now) {
        alertWindows.entrySet().removeIf(entry ->
            entry.getValue().startedAt().plus(RATE_LIMIT_WINDOW).isBefore(now));
    }

    private String activeProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length == 0 ? "default" : activeProfiles[0];
    }

    private SimpleClientHttpRequestFactory createRequestFactory(DiscordAlertProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = Math.toIntExact(Duration.ofSeconds(properties.getTimeoutSeconds()).toMillis());
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return requestFactory;
    }

    private record AlertWindow(Instant startedAt, int count) {
    }
}
