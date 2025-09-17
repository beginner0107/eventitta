package com.eventitta.common.notification.service;

import com.eventitta.common.notification.constants.AlertConstants;
import com.eventitta.common.notification.domain.AlertLevel;
import com.eventitta.common.notification.domain.SlackAttachment;
import com.eventitta.common.notification.domain.SlackField;
import com.eventitta.common.notification.domain.SlackMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SlackMessageBuilder {

    private static final int MAX_STACK_TRACE_LINES = 3;
    private static final String ALERT_TITLE_FORMAT = "%s - %s [%s]";
    private static final String LINE_SEPARATOR = System.lineSeparator();

    public SlackMessage buildMessage(AlertLevel level, String errorCode, String message,
                                     String requestUri, String userInfo, Throwable exception,
                                     String environment, String channel, String username) {

        String color = getColorForLevel(level);
        String title = String.format(ALERT_TITLE_FORMAT, errorCode, level, environment);

        List<SlackField> fields = buildFields(requestUri, userInfo, exception, level);

        SlackAttachment attachment = SlackAttachment.builder()
            .color(color)
            .title(title)
            .text(message)
            .fields(fields)
            .ts(System.currentTimeMillis() / 1000)
            .build();

        return SlackMessage.builder()
            .channel(channel)
            .username(username)
            .text(String.format(AlertConstants.ALERT_TEXT_FORMAT, AlertConstants.ALERT_EMOJI, level))
            .attachments(List.of(attachment))
            .build();
    }

    private List<SlackField> buildFields(String requestUri, String userInfo,
                                         Throwable exception, AlertLevel level) {
        List<SlackField> fields = new ArrayList<>();
        String REQUEST_URL = "Request URI";
        String USER = "User";
        String EXCEPTION = "Exception";

        if (StringUtils.hasText(requestUri)) {
            fields.add(SlackField.builder()
                .title(REQUEST_URL)
                .value(requestUri)
                .build());
        }

        if (StringUtils.hasText(userInfo)) {
            fields.add(SlackField.builder()
                .title(USER)
                .value(userInfo)
                .build());
        }

        if (exception != null && level == AlertLevel.CRITICAL) {
            fields.add(SlackField.builder()
                .title(EXCEPTION)
                .value(getShortStackTrace(exception))
                .isShort(false)
                .build());
        }

        return fields;
    }

    private String getColorForLevel(AlertLevel level) {
        return level.getColor();
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
