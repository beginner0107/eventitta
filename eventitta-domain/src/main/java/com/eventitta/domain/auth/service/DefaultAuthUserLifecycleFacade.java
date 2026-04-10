package com.eventitta.domain.auth.service;

import com.eventitta.domain.auth.api.internal.facade.AuthUserLifecycleFacade;
import com.eventitta.domain.auth.repository.AuthIdentityRepository;
import com.eventitta.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class DefaultAuthUserLifecycleFacade implements AuthUserLifecycleFacade {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthIdentityRepository authIdentityRepository;

    @Override
    @Transactional
    public void revokeAllSessions(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void cleanupDeletedUser(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        authIdentityRepository.deleteByUserId(userId);
    }
}
