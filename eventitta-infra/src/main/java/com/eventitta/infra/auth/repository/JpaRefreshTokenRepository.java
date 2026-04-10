package com.eventitta.infra.auth.repository;

import com.eventitta.domain.auth.domain.RefreshToken;
import com.eventitta.domain.auth.repository.RefreshTokenRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaRefreshTokenRepository extends JpaRepository<RefreshToken, Long>, RefreshTokenRepository {
    Optional<RefreshToken> findByTokenKey(String tokenKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rt from RefreshToken rt where rt.tokenKey = :tokenKey")
    Optional<RefreshToken> findByTokenKeyForUpdate(@Param("tokenKey") String tokenKey);

    List<RefreshToken> findAllByUserId(Long userId);

    List<RefreshToken> findAllByUserIdOrderByLastSeenAtDesc(Long userId);

    Optional<RefreshToken> findByUserIdAndSessionId(Long userId, String sessionId);

    long countByUserId(Long userId);

    long deleteByExpiresAtBefore(LocalDateTime now);

    @Modifying
    @Transactional
    @Query(
        value = "DELETE FROM refresh_tokens WHERE user_id = :userId",
        nativeQuery = true
    )
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(
        value = "DELETE FROM refresh_tokens WHERE user_id = :userId AND session_id = :sessionId",
        nativeQuery = true
    )
    void deleteByUserIdAndSessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);
}
