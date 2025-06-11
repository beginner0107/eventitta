package com.eventitta.auth.schedule;

import com.eventitta.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupTaskTest {

    @Mock
    RefreshTokenRepository repository;

    @InjectMocks
    RefreshTokenCleanupTask task;

    @Test
    @DisplayName("만료된 리프레시 토큰을 삭제한다")
    void removeExpiredRefreshTokens_deletesTokens() {
        given(repository.deleteByExpiresAtBefore(any())).willReturn(2L);

        task.removeExpiredRefreshTokens();

        verify(repository).deleteByExpiresAtBefore(any());
    }
}
