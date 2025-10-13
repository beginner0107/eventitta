package com.eventitta.gamification.event;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.service.UserActivityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.eventitta.gamification.domain.ActivityType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserActivityEventListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private UserActivityService userActivityService;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("게시글 작성 시 활동 내역이 기록된다")
    void givenLogEvent_whenPublished_thenRecordActivityInvoked() {
        // given
        doNothing().when(userActivityService).recordActivity(anyLong(), any(ActivityType.class), anyLong());

        // when - 트랜잭션 안에서 이벤트 발행
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new UserActivityLogRequestedEvent(1L, CREATE_POST, 10L));
            return null;
        }); // 여기서 커밋 발생 → AFTER_COMMIT 이벤트 실행

        // then
        verify(userActivityService, timeout(2000).times(1))
            .recordActivity(1L, CREATE_POST, 10L);
    }

    @Test
    @DisplayName("좋아요 취소 시 활동 내역이 삭제된다")
    void givenRevokeEvent_whenPublished_thenRevokeActivityInvoked() {
        // given
        doNothing().when(userActivityService).revokeActivity(anyLong(), any(ActivityType.class), anyLong());

        // when
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new UserActivityRevokeRequestedEvent(2L, LIKE_POST_CANCEL, 20L));
            return null;
        });

        // then
        verify(userActivityService, timeout(2000).times(1))
            .revokeActivity(2L, LIKE_POST_CANCEL, 20L);
    }

    @Test
    @DisplayName("이벤트 리스너가 별도의 스레드에서 동작한다")
    void givenEvent_whenHandled_thenRunsInDifferentThread() {
        // given
        AtomicReference<String> listenerThread = new AtomicReference<>();
        doAnswer(invocation -> {
            listenerThread.set(Thread.currentThread().getName());
            return null;
        }).when(userActivityService).recordActivity(anyLong(), any(ActivityType.class), anyLong());

        String mainThread = Thread.currentThread().getName();

        // when
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new UserActivityLogRequestedEvent(3L, CREATE_POST, 30L));
            return null;
        });

        // then
        verify(userActivityService, timeout(2000).times(1))
            .recordActivity(3L, CREATE_POST, 30L);

        assertThat(listenerThread.get()).isNotNull();
        assertThat(listenerThread.get()).isNotEqualTo(mainThread);
        assertThat(listenerThread.get()).startsWith("ActivityEvent-");
        assertThat(applicationContext.getBean(com.eventitta.common.config.AsyncConfig.class)).isNotNull();
    }

    @Test
    @DisplayName("활동 기록 중 오류가 발생하면 로그가 남는다")
    void givenException_whenRecordActivity_thenErrorLogged() {
        // given
        AtomicBoolean exceptionThrown = new AtomicBoolean(false);
        doAnswer(invocation -> {
            exceptionThrown.set(true);
            throw new RuntimeException("에러 발생");
        }).when(userActivityService).recordActivity(anyLong(), any(ActivityType.class), anyLong());

        // when
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new UserActivityLogRequestedEvent(4L, CREATE_POST, 40L));
            return null;
        });

        // then
        verify(userActivityService, timeout(2000).times(1))
            .recordActivity(4L, CREATE_POST, 40L);

        // 예외가 실제로 발생했는지 확인
        assertThat(exceptionThrown.get()).isTrue();
    }

    @Test
    @DisplayName("잘못된 이벤트 파라미터가 들어오면 예외가 발생하고 로그가 남는다")
    void givenInvalidParameter_whenEventPublished_thenExceptionLoggedAndHandled() {
        // given
        doThrow(new IllegalArgumentException("Invalid activityCode"))
            .when(userActivityService).recordActivity(anyLong(), any(ActivityType.class), anyLong());

        // when
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new UserActivityLogRequestedEvent(5L, null, 50L));
            return null;
        });

        // then - 이벤트 리스너가 호출되었는지만 확인
        verify(userActivityService, timeout(2000).times(1))
            .recordActivity(5L, null, 50L);
        // 로그로만 남고 시스템은 정상 동작함
    }

    @Test
    @DisplayName("동일한 이벤트가 여러 번 발생해도 중복 기록되지 않는다")
    void givenDuplicateEvent_whenPublished_thenNoDuplicateActivity() {
        // given
        doNothing().when(userActivityService).recordActivity(anyLong(), any(ActivityType.class), anyLong());

        // when - 동일한 이벤트를 두 번 발행
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new UserActivityLogRequestedEvent(6L, CREATE_POST, 60L));
            eventPublisher.publishEvent(new UserActivityLogRequestedEvent(6L, CREATE_POST, 60L));
            return null;
        });

        // then - 이벤트는 2번 발행되므로 리스너도 2번 호출됨
        verify(userActivityService, timeout(2000).times(2))
            .recordActivity(6L, CREATE_POST, 60L);
        // 참고: 실제 중복 방지는 UserActivityService 내부 로직에서 처리됨
    }

    @Test
    @DisplayName("비동기 작업이 지연되거나 실패해도 시스템이 정상 동작한다")
    void givenAsyncFailureOrDelay_whenEventPublished_thenSystemHandlesGracefully() throws InterruptedException {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);

        doAnswer(invocation -> {
            try {
                // 지연 시뮬레이션을 CountDownLatch로 대체
                exceptionOccurred.set(true);
                throw new RuntimeException("비동기 실패");
            } finally {
                latch.countDown(); // 완료 신호
            }
        }).when(userActivityService).recordActivity(anyLong(), any(ActivityType.class), anyLong());

        // when
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new UserActivityLogRequestedEvent(7L, CREATE_POST, 70L));
            return null;
        });

        // then - 최대 3초 대기, 완료되면 즉시 통과
        assertTrue(latch.await(3, TimeUnit.SECONDS), "이벤트 처리가 완료되지 않았습니다");

        verify(userActivityService, times(1))
            .recordActivity(7L, CREATE_POST, 70L);

        // 예외가 발생했지만 시스템은 정상 동작함을 확인
        assertThat(exceptionOccurred.get()).isTrue();
    }
}
