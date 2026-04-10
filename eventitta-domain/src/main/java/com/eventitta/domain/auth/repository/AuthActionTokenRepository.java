package com.eventitta.domain.auth.repository;

import com.eventitta.domain.auth.domain.AuthActionToken;
import com.eventitta.domain.auth.domain.AuthActionTokenPurpose;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthActionTokenRepository {

    Optional<AuthActionToken> findByTokenKey(String tokenKey);

    Optional<AuthActionToken> findByTokenKeyForUpdate(String tokenKey);

    void deleteByUserIdAndPurpose(Long userId, AuthActionTokenPurpose purpose);

    long deleteByExpiresAtBefore(LocalDateTime now);

    AuthActionToken save(AuthActionToken authActionToken);
}
