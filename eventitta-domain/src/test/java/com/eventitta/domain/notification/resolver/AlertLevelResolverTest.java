package com.eventitta.domain.notification.resolver;

import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.common.exception.ErrorCode;
import com.eventitta.domain.common.exception.ErrorStatus;
import com.eventitta.domain.notification.domain.AlertLevel;
import java.net.ConnectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("시스템 오류 발생 시 알림 등급을 자동으로 결정하는 기능")
class AlertLevelResolverTest {

    private AlertLevelResolver alertLevelResolver;

    @BeforeEach
    void setUp() {
        alertLevelResolver = new AlertLevelResolver();
    }

    @Test
    @DisplayName("DB 연결 실패는 CRITICAL로 분류한다")
    void shouldResolveToCriticalForDataAccessException() {
        DataAccessException exception = new DataAccessException("연결 거부") {
        };

        AlertLevel level = alertLevelResolver.resolveLevel(exception);

        assertThat(level).isEqualTo(AlertLevel.CRITICAL);
    }

    @Test
    @DisplayName("500 계열 CustomException은 HIGH로 분류한다")
    void shouldResolveToHighFor5xxCustomException() {
        ErrorCode mockErrorCode = mock(ErrorCode.class);
        when(mockErrorCode.defaultHttpStatus()).thenReturn(ErrorStatus.INTERNAL_SERVER_ERROR);

        AlertLevel level = alertLevelResolver.resolveLevel(new CustomException(mockErrorCode));

        assertThat(level).isEqualTo(AlertLevel.HIGH);
    }

    @Test
    @DisplayName("400 계열 CustomException은 MEDIUM으로 분류한다")
    void shouldResolveToMediumFor4xxCustomException() {
        ErrorCode mockErrorCode = mock(ErrorCode.class);
        when(mockErrorCode.defaultHttpStatus()).thenReturn(ErrorStatus.BAD_REQUEST);

        AlertLevel level = alertLevelResolver.resolveLevel(new CustomException(mockErrorCode));

        assertThat(level).isEqualTo(AlertLevel.MEDIUM);
    }

    @Test
    @DisplayName("기타 예외는 INFO로 분류한다")
    void shouldResolveToInfoForGenericRuntimeException() {
        AlertLevel level = alertLevelResolver.resolveLevel(new RuntimeException("generic"));

        assertThat(level).isEqualTo(AlertLevel.INFO);
    }

    @Test
    @DisplayName("연결 거부 메시지는 CRITICAL로 분류한다")
    void shouldResolveToCriticalForConnectException() {
        AlertLevel level = alertLevelResolver.resolveLevel(new ConnectException("Connection failed"));

        assertThat(level).isEqualTo(AlertLevel.CRITICAL);
    }
}
