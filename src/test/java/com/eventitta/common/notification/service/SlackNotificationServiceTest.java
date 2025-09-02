package com.eventitta.common.notification.service;

import com.eventitta.common.notification.config.SlackProperties;
import com.eventitta.common.notification.domain.AlertLevel;
import com.eventitta.common.notification.domain.SlackMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("슬랙 알림 서비스 핵심 기능")
@ExtendWith(MockitoExtension.class)
class SlackNotificationServiceTest {

    @Mock
    private SlackProperties slackProperties;
    @Mock
    private RateLimiter rateLimiter;
    @Mock
    private RestClient slackRestClient;
    @Mock
    private Environment environment;
    @Mock
    private SlackMessageBuilder messageBuilder;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private SlackNotificationService slackNotificationService;

    @BeforeEach
    void setUp() {
        slackNotificationService = new SlackNotificationService(
            slackProperties, rateLimiter, slackRestClient, environment, messageBuilder
        );
    }

    @DisplayName("슬랙 비활성화 시에는 전송 하지 않는다")
    @Test
    void shouldNotSendWhenSlackIsDisabled() {
        // given
        when(slackProperties.isEnabled()).thenReturn(false);

        // when
        slackNotificationService.sendAlert(
            AlertLevel.CRITICAL, "TEST_ERROR", "Test message",
            "/api/test", "user123", new RuntimeException("Test")
        );

        // then
        verifyNoInteractions(rateLimiter, messageBuilder, slackRestClient);
    }

    @DisplayName("요청 횟수 제한인 경우에는 예외를 발송하지 않는다")
    @Test
    void shouldNotSendWhenRateLimited() {
        // given
        when(slackProperties.isEnabled()).thenReturn(true);
        when(rateLimiter.shouldSendAlert("TEST_ERROR", AlertLevel.HIGH)).thenReturn(false);

        // when
        slackNotificationService.sendAlert(
            AlertLevel.HIGH, "TEST_ERROR", "Test message",
            "/api/test", "user123", null
        );

        // then
        verify(rateLimiter).shouldSendAlert("TEST_ERROR", AlertLevel.HIGH);
        verifyNoInteractions(messageBuilder, slackRestClient);
    }

    @DisplayName("정상 조건에서 슬랙 전송을 성공한다")
    @Test
    void shouldSendSlackAlertWhenConditionsMet() {
        // given
        setupSuccessfulSlackSend();

        SlackMessage expectedMessage = SlackMessage.builder()
            .channel("#alerts")
            .username("eventitta-bot")
            .text("Test alert")
            .build();

        when(messageBuilder.buildMessage(
            eq(AlertLevel.CRITICAL), eq("API_ERROR"), eq("Database connection failed"),
            eq("/api/users"), eq("user123@example.com"), any(RuntimeException.class),
            eq("local"), eq("#alerts"), eq("eventitta-bot")
        )).thenReturn(expectedMessage);

        // when
        slackNotificationService.sendAlert(
            AlertLevel.CRITICAL, "API_ERROR", "Database connection failed",
            "/api/users", "user123@example.com", new RuntimeException("Connection timeout")
        );

        // then
        verify(rateLimiter).shouldSendAlert("API_ERROR", AlertLevel.CRITICAL);
        verify(messageBuilder).buildMessage(
            eq(AlertLevel.CRITICAL), eq("API_ERROR"), eq("Database connection failed"),
            eq("/api/users"), eq("user123@example.com"), any(RuntimeException.class),
            eq("local"), eq("#alerts"), eq("eventitta-bot")
        );
        verify(slackRestClient).post();
        verify(requestBodyUriSpec).uri("https://hooks.slack.com/test");
        verify(requestBodySpec).body(expectedMessage);
    }

    @DisplayName("네트워크 오류가 발생해도 애플리케이션이 중단되지 않는다")
    @Test
    void shouldHandleSlackSendException() {
        // given
        setupSlackSendWithNetworkError();

        // when - 예외가 발생해도 정상 종료되어야 함
        slackNotificationService.sendAlert(
            AlertLevel.HIGH, "ERROR_CODE", "Test error",
            "/api/test", "user", new RuntimeException("Original error")
        );

        // then
        verify(rateLimiter).shouldSendAlert("ERROR_CODE", AlertLevel.HIGH);
        verify(messageBuilder).buildMessage(
            any(), anyString(), anyString(), anyString(), anyString(), any(),
            anyString(), anyString(), anyString()
        );
        verify(slackRestClient).post();
    }

    private void setupSuccessfulSlackSend() {
        when(slackProperties.isEnabled()).thenReturn(true);
        when(slackProperties.getWebhookUrl()).thenReturn("https://hooks.slack.com/test");
        when(slackProperties.getChannel()).thenReturn("#alerts");
        when(slackProperties.getUsername()).thenReturn("eventitta-bot");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        when(rateLimiter.shouldSendAlert("API_ERROR", AlertLevel.CRITICAL)).thenReturn(true);

        // RestClient mock chain
        when(slackRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);
    }

    private void setupSlackSendWithNetworkError() {
        when(slackProperties.isEnabled()).thenReturn(true);
        when(slackProperties.getWebhookUrl()).thenReturn("https://hooks.slack.com/test");
        when(slackProperties.getChannel()).thenReturn("#alerts");
        when(slackProperties.getUsername()).thenReturn("eventitta-bot");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        when(rateLimiter.shouldSendAlert("ERROR_CODE", AlertLevel.HIGH)).thenReturn(true);

        SlackMessage mockMessage = SlackMessage.builder().build();
        when(messageBuilder.buildMessage(
            any(), anyString(), anyString(), anyString(), anyString(), any(),
            anyString(), anyString(), anyString()
        )).thenReturn(mockMessage);

        // RestClient에서 예외 발생 설정
        when(slackRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("https://hooks.slack.com/test")).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenThrow(new RuntimeException("Network error"));
    }
}
