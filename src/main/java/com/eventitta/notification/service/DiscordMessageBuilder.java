package com.eventitta.notification.service;

import com.eventitta.notification.constants.AlertConstants;
import com.eventitta.notification.domain.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DiscordMessageBuilder {

    private static final int MAX_STACK_TRACE_LINES = 3;
    private static final String ALERT_TITLE_FORMAT = "%s - %s [%s]";
    private static final String LINE_SEPARATOR = System.lineSeparator();

    public DiscordMessage buildMessage(AlertLevel level, String errorCode, String message,
                                       String requestUri, String userInfo, Throwable exception,
                                       String environment, String username) {

        int color = level.getDiscordColor();
        String title = String.format(ALERT_TITLE_FORMAT, errorCode, level, environment);

        List<DiscordField> fields = buildFields(requestUri, userInfo, exception, level);

        DiscordEmbed embed = DiscordEmbed.builder()
            .title(title)
            .description(message)
            .color(color)
            .fields(fields)
            .footer(DiscordFooter.builder()
                .text(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build())
            .build();

        String content = String.format(AlertConstants.ALERT_TEXT_FORMAT,
            getEmojiForLevel(level), level);

        return DiscordMessage.builder()
            .content(content)
            .username(username)
            .embeds(List.of(embed))
            .build();
    }

    private List<DiscordField> buildFields(String requestUri, String userInfo,
                                           Throwable exception, AlertLevel level) {
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
                .value("```" + getShortStackTrace(exception) + "```")
                .inline(false)
                .build());
        }

        return fields;
    }

    private String getEmojiForLevel(AlertLevel level) {
        return switch (level) {
            case CRITICAL -> "🚨";
            case HIGH -> "⚠️";
            case MEDIUM -> "⚡";
            case INFO -> "ℹ️";
        };
    }

    private String getShortStackTrace(Throwable exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String stackTrace = sw.toString();

        return Arrays.stream(stackTrace.split(LINE_SEPARATOR))
            .limit(MAX_STACK_TRACE_LINES)
            .collect(Collectors.joining(LINE_SEPARATOR));
    }
}
