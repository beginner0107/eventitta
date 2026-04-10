package com.eventitta.infra.auth.repository;

import com.eventitta.domain.auth.domain.AuthIdentity;
import com.eventitta.domain.auth.domain.AuthProvider;
import com.eventitta.domain.auth.repository.AuthIdentityRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaAuthIdentityRepository extends JpaRepository<AuthIdentity, Long>, AuthIdentityRepository {

    Optional<AuthIdentity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    Optional<AuthIdentity> findByUserIdAndProvider(Long userId, AuthProvider provider);

    List<AuthIdentity> findAllByUserId(Long userId);

    long countByUserId(Long userId);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndProvider(Long userId, AuthProvider provider);
}
