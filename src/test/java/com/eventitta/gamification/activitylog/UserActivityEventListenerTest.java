package com.eventitta.gamification.activitylog;

import com.eventitta.gamification.constant.ActivityCodes;
import com.eventitta.gamification.service.UserActivityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
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
    private PlatformTransactionManager txManager;

    @MockitoSpyBean
    private UserActivityService userActivityService;

    @Autowired
    private ApplicationContext applicationContext;


    @Test
    @DisplayName("게시글 작성 요청 시 트랜잭션이 커밋되어야만 활동 로그가 남는다")
    void givenLogEvent_whenCommitted_thenRecordActivityInvoked() {
        doNothing().when(userActivityService).recordActivity(anyLong(), anyString(), anyLong());

        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new UserActivityLogRequestedEvent(1L, ActivityCodes.CREATE_POST, 10L));
            verify(userActivityService, never()).recordActivity(anyLong(), anyString(), anyLong());
        });

        verify(userActivityService, timeout(1000).times(1))
            .recordActivity(1L, ActivityCodes.CREATE_POST, 10L);
    }

    @Test
    @DisplayName("좋아요 취소 시 트랜잭션 커밋이 완료된 후 활동 로그가 삭제된다")
    void givenRevokeEvent_whenCommitted_thenRevokeActivityInvoked() {
        doNothing().when(userActivityService).revokeActivity(anyLong(), anyString(), anyLong());

        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new UserActivityRevokeRequestedEvent(2L, ActivityCodes.LIKE_POST, 20L));
            verify(userActivityService, never()).revokeActivity(anyLong(), anyString(), anyLong());
        });

        verify(userActivityService, timeout(1000).times(1))
            .revokeActivity(2L, ActivityCodes.LIKE_POST, 20L);
    }

    @Test
    @DisplayName("트랜잭션 롤백 시 활동 로그가 남지 않는다")
    void givenLogEvent_whenRollback_thenRecordActivityNotInvoked() throws InterruptedException {
        doNothing().when(userActivityService).recordActivity(anyLong(), anyString(), anyLong());

        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new UserActivityLogRequestedEvent(3L, ActivityCodes.CREATE_POST, 30L));
            verify(userActivityService, never()).recordActivity(anyLong(), anyString(), anyLong());
            status.setRollbackOnly();
        });

        verify(userActivityService, never()).recordActivity(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("이벤트 리스너는 별도 스레드에서 실행된다")
    void givenEvent_whenHandled_thenRunsInDifferentThread() {
        AtomicReference<String> listenerThread = new AtomicReference<>();
        doAnswer(invocation -> {
            listenerThread.set(Thread.currentThread().getName());
            return null;
        }).when(userActivityService).recordActivity(anyLong(), anyString(), anyLong());

        AtomicReference<String> txThread = new AtomicReference<>();
        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.executeWithoutResult(status -> {
            txThread.set(Thread.currentThread().getName());
            eventPublisher.publishEvent(new UserActivityLogRequestedEvent(3L, ActivityCodes.CREATE_POST, 30L));
        });

        verify(userActivityService, timeout(1000).times(1)).recordActivity(3L, ActivityCodes.CREATE_POST, 30L);

        assertThat(listenerThread.get()).isNotNull();
        assertThat(txThread.get()).isNotNull();
        assertThat(listenerThread.get()).isNotEqualTo(txThread.get());
        assertThat(listenerThread.get()).startsWith("ActivityEvent-");
        assertThat(applicationContext.getBean(com.eventitta.common.config.AsyncConfig.class)).isNotNull();
    }
}
