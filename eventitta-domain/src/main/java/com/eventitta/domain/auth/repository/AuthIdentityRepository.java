package com.eventitta.domain.auth.repository;

import com.eventitta.domain.auth.domain.AuthIdentity;
import com.eventitta.domain.auth.domain.AuthProvider;

import java.util.List;
import java.util.Optional;

public interface AuthIdentityRepository {

    Optional<AuthIdentity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    Optional<AuthIdentity> findByUserIdAndProvider(Long userId, AuthProvider provider);

    List<AuthIdentity> findAllByUserId(Long userId);

    long countByUserId(Long userId);

    AuthIdentity save(AuthIdentity authIdentity);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndProvider(Long userId, AuthProvider provider);
}
