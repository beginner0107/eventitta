package com.eventitta.infra.notification.service;

import com.eventitta.domain.notification.constants.AlertConstants;
import com.eventitta.domain.notification.domain.AlertLevel;
import com.eventitta.domain.notification.domain.DiscordEmbed;
import com.eventitta.domain.notification.domain.DiscordField;
import com.eventitta.domain.notification.domain.DiscordFooter;
import com.eventitta.domain.notification.domain.DiscordMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DiscordAlertMessageBuilder {

    private static final int MAX_STACK_TRACE_LINES = 3;
    private static final String ALERT_TITLE_FORMAT = "%s - %s [%s]";
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Clock clock;

    public DiscordAlertMessageBuilder(Clock clock) {
        this.clock = clock;
    }

    public DiscordMessage build(
        AlertLevel level,
        String errorCode,
        String message,
        String requestUri,
        String userInfo,
        Throwable exception,
        String environment,
        String username
    ) {
        DiscordEmbed embed = DiscordEmbed.builder()
            .title(String.format(ALERT_TITLE_FORMAT, errorCode, level, environment))
            .description(message)
            .color(level.getDiscordColor())
            .fields(buildFields(level, requestUri, userInfo, exception))
            .footer(DiscordFooter.builder()
                .text(LocalDateTime.now(clock).format(TIMESTAMP_FORMAT))
                .build())
            .build();

        return DiscordMessage.builder()
            .content(String.format(AlertConstants.ALERT_TEXT_FORMAT, level.name(), "notification"))
            .username(username)
            .embeds(List.of(embed))
            .build();
    }

    private List<DiscordField> buildFields(
        AlertLevel level,
        String requestUri,
        String userInfo,
        Throwable exception
    ) {
        List<DiscordField> fields = new ArrayList<>();

        if (StringUtils.hasText(requestUri)) {
            fields.add(DiscordField.builder()
                .name("Request URI")
                .value(requestUri)
                .inline(true)
                .build());
        }

        if (StringUtils.hasText(userInfo)) {
            fields.add(DiscordField.builder()
                .name("User")
                .value(userInfo)
                .inline(true)
                .build());
        }

        if (exception != null && level == AlertLevel.CRITICAL) {
            fields.add(DiscordField.builder()
                .name("Exception")
                .value("```" + shortenStackTrace(exception) + "```")
                .inline(false)
                .build());
        }

        return fields;
    }

    private String shortenStackTrace(Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return Arrays.stream(stringWriter.toString().split(LINE_SEPARATOR))
            .limit(MAX_STACK_TRACE_LINES)
            .collect(Collectors.joining(LINE_SEPARATOR));
    }
}
