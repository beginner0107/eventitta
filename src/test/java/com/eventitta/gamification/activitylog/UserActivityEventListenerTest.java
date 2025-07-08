package com.eventitta.gamification.activitylog;

import com.eventitta.gamification.constant.ActivityCodes;
import com.eventitta.gamification.service.UserActivityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
}
