package com.eventitta.domain.auth.repository;

import com.eventitta.domain.auth.domain.RefreshToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByTokenKey(String tokenKey);

    Optional<RefreshToken> findByTokenKeyForUpdate(String tokenKey);

    List<RefreshToken> findAllByUserId(Long userId);

    List<RefreshToken> findAllByUserIdOrderByLastSeenAtDesc(Long userId);

    Optional<RefreshToken> findByUserIdAndSessionId(Long userId, String sessionId);

    long countByUserId(Long userId);

    long deleteByExpiresAtBefore(LocalDateTime now);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndSessionId(Long userId, String sessionId);

    RefreshToken save(RefreshToken refreshToken);

    RefreshToken saveAndFlush(RefreshToken refreshToken);

    void delete(RefreshToken refreshToken);

    Optional<RefreshToken> findById(Long id);

    List<RefreshToken> findAll();
}
