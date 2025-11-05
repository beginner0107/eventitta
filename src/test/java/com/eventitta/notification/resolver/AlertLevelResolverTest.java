package com.eventitta.notification.resolver;

import com.eventitta.common.exception.CustomException;
import com.eventitta.common.exception.ErrorCode;
import com.eventitta.notification.domain.AlertLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;

import java.net.ConnectException;

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

    @DisplayName("데이터베이스가 연결되지 않으면 'CRITICAL' 등급으로 분류한다")
    @Test
    void shouldResolveToCriticalForDataAccessException() {
        // given
        DataAccessException exception = new DataAccessException("연결 거부") {
        };

        // when
        AlertLevel level = alertLevelResolver.resolveLevel(exception);

        // then
        assertThat(level).isEqualTo(AlertLevel.CRITICAL);
    }

    @DisplayName("서버가 연결을 거부하면 '매우 위험' 등급으로 분류한다")
    @Test
    void shouldResolveToCriticalForConnectException() {
        // given
        ConnectException exception = new ConnectException("연결 거부");

        // when
        AlertLevel level = alertLevelResolver.resolveLevel(exception);

        // then
        assertThat(level).isEqualTo(AlertLevel.CRITICAL);
    }

    @DisplayName("'Connection 관련' 오류는 'CRITICAL' 등급으로 분류한다")
    @Test
    void shouldResolveToCriticalForConnectionRefusedMessage() {
        // given
        ConnectException exception = new ConnectException("Connection failed");

        // when
        AlertLevel level = alertLevelResolver.resolveLevel(exception);

        // then
        assertThat(level).isEqualTo(AlertLevel.CRITICAL);
    }

    @DisplayName("서버 내부에서 문제가 생기면(500번 오류) '위험' 등급으로 분류한다")
    @Test
    void shouldResolveToHighFor5xxCustomException() {
        // given
        ErrorCode mockErrorCode = mock(ErrorCode.class);
        when(mockErrorCode.defaultHttpStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        CustomException exception = new CustomException(mockErrorCode);

        // when
        AlertLevel level = alertLevelResolver.resolveLevel(exception);

        // then
        assertThat(level).isEqualTo(AlertLevel.HIGH);
    }

    @DisplayName("사용자가 잘못 요청하면(400번 오류) 'MEDIUM' 등급으로 분류한다")
    @Test
    void shouldResolveToMediumFor4xxCustomException() {
        // given
        ErrorCode mockErrorCode = mock(ErrorCode.class);
        when(mockErrorCode.defaultHttpStatus()).thenReturn(HttpStatus.BAD_REQUEST);

        CustomException exception = new CustomException(mockErrorCode);

        // when
        AlertLevel level = alertLevelResolver.resolveLevel(exception);

        // then
        assertThat(level).isEqualTo(AlertLevel.MEDIUM);
    }

    @DisplayName("정상 처리되었지만 기록이 필요한 경우 'INFO' 등급으로 분류한다")
    @Test
    void shouldResolveToInfoFor2xxCustomException() {
        // given
        ErrorCode mockErrorCode = mock(ErrorCode.class);
        when(mockErrorCode.defaultHttpStatus()).thenReturn(HttpStatus.OK);

        CustomException exception = new CustomException(mockErrorCode);

        // when
        AlertLevel level = alertLevelResolver.resolveLevel(exception);

        // then
        assertThat(level).isEqualTo(AlertLevel.INFO);
    }

    @DisplayName("일반적인 프로그램 오류는 'INFO' 등급으로 분류한다")
    @Test
    void shouldResolveToInfoForGenericRuntimeException() {
        // given
        RuntimeException exception = new RuntimeException();

        // when
        AlertLevel level = alertLevelResolver.resolveLevel(exception);

        // then
        assertThat(level).isEqualTo(AlertLevel.INFO);
    }

    @DisplayName("입력값이 잘못된 경우 'INFO' 등급으로 분류한다")
    @Test
    void shouldResolveToInfoForIllegalArgumentException() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("유효하지 않은 파라미터");

        // when
        AlertLevel level = alertLevelResolver.resolveLevel(exception);

        // then
        assertThat(level).isEqualTo(AlertLevel.INFO);
    }

    @DisplayName("오류 설명이 없어도 기본적으로 'INFO' 등급으로 분류한다")
    @Test
    void shouldHandleExceptionWithNullMessage() {
        // given
        RuntimeException exception = new RuntimeException((String) null);

        // when
        AlertLevel level = alertLevelResolver.resolveLevel(exception);

        // then
        assertThat(level).isEqualTo(AlertLevel.INFO);
    }

    @DisplayName("'연결 거부' 메시지가 없는 연결 오류는 'INFO' 등급으로 분류한다")
    @Test
    void shouldNotBeCriticalForNonConnectionRefusedMessage() {
        // given
        RuntimeException exception = new RuntimeException("연결 오류");

        // when
        AlertLevel level = alertLevelResolver.resolveLevel(exception);

        // then
        assertThat(level).isEqualTo(AlertLevel.INFO);
    }

    @DisplayName("여러 조건에 해당하는 오류는 가장 심각한 등급으로 분류한다")
    @Test
    void shouldApplyCorrectPriorityForComplexConditions() {
        // given
        ErrorCode mockErrorCode = mock(ErrorCode.class);
        when(mockErrorCode.defaultHttpStatus()).thenReturn(HttpStatus.BAD_REQUEST);

        CustomException exception = new CustomException(mockErrorCode) {
        };
        DataAccessException dataException = new DataAccessException("DB error", exception) {
        };

        // when
        AlertLevel level = alertLevelResolver.resolveLevel(dataException);

        // then
        assertThat(level).isEqualTo(AlertLevel.CRITICAL);
    }

    @DisplayName("페이지 이동 안내(300번) 응답은 'INFO' 등급으로 분류한다")
    @Test
    void shouldResolveToInfoFor3xxCustomException() {
        // given
        ErrorCode mockErrorCode = mock(ErrorCode.class);
        when(mockErrorCode.defaultHttpStatus()).thenReturn(HttpStatus.MOVED_PERMANENTLY);

        CustomException exception = new CustomException(mockErrorCode);

        // when
        AlertLevel level = alertLevelResolver.resolveLevel(exception);

        // then
        assertThat(level).isEqualTo(AlertLevel.INFO);
    }
}
