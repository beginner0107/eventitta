package com.eventitta.common.notification.service;

import com.eventitta.common.notification.domain.AlertLevel;
import com.eventitta.common.notification.domain.SlackMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("슬랙 메시지 빌더 기본 기능")
class SlackMessageBuilderTest {

    private SlackMessageBuilder messageBuilder;

    @BeforeEach
    void setUp() {
        messageBuilder = new SlackMessageBuilder();
    }

    @DisplayName("메시지 텍스트와 컬러가 레벨에 맞게 설정된다")
    @Test
    void shouldFormatMessageAccordingToLevel() {
        // given
        AlertLevel[] levels = {AlertLevel.CRITICAL, AlertLevel.HIGH, AlertLevel.MEDIUM, AlertLevel.INFO};
        String errorCode = "ERROR";
        String body = "Test";

        // when & then
        for (AlertLevel level : levels) {
            SlackMessage msg = messageBuilder.buildMessage(
                level, errorCode, body, null, null, null, "env", "#chan", "bot"
            );
            String expectedText = ":warning: " + level + " Alert";
            String expectedColor = level.getColor();
            assertThat(msg.text()).isEqualTo(expectedText);
            assertThat(msg.attachments()).hasSize(1);
            assertThat(msg.attachments().get(0).color()).isEqualTo(expectedColor);
        }
    }

    @DisplayName("Critical 레벨에서만 예외 정보가 포함되고, 그 외에는 제외된다")
    @Test
    void shouldIncludeOrExcludeException() {
        // given
        Exception ex = new RuntimeException("fail");

        // when
        SlackMessage criticalMsg = messageBuilder.buildMessage(
            AlertLevel.CRITICAL, "ERR", "msg", null, null, ex, "env", "#c", "b"
        );
        SlackMessage infoMsg = messageBuilder.buildMessage(
            AlertLevel.INFO, "ERR", "msg", null, null, ex, "env", "#c", "b"
        );

        // then
        assertThat(criticalMsg.attachments().get(0).fields()).anySatisfy(f ->
            assertThat(f.getTitle()).isEqualTo("Exception")
        );
        assertThat(infoMsg.attachments().get(0).fields()).isEmpty();
    }
}
