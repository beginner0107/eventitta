package com.eventitta.domain.auth.api.internal.facade;

public interface AuthUserLifecycleFacade {

    void revokeAllSessions(Long userId);

    void cleanupDeletedUser(Long userId);
}
