package com.eventitta.gamification.activitylog;

import com.eventitta.gamification.constant.ActivityCodes;
import com.eventitta.gamification.service.UserActivityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(OutputCaptureExtension.class)
class UserActivityEventListenerErrorTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager txManager;

    @MockitoBean
    private UserActivityService userActivityService;

    @Test
    @DisplayName("활동 기록 실패 시 예외가 무시되고 에러 로그가 남는다")
    void givenRecordActivityThrows_whenHandleEvent_thenLogged(CapturedOutput output) {
        doThrow(new IllegalStateException("boom")).when(userActivityService)
            .recordActivity(anyLong(), anyString(), anyLong());

        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.executeWithoutResult(status ->
            eventPublisher.publishEvent(new UserActivityLogRequestedEvent(1L, ActivityCodes.CREATE_POST, 10L))
        );

        verify(userActivityService, timeout(1000).times(1))
            .recordActivity(1L, ActivityCodes.CREATE_POST, 10L);
        assertThat(output.getOut()).contains("[활동 기록 실패]");
    }
}
