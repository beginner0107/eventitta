package com.eventitta.auth.repository;

import com.eventitta.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUserId(Long userId);

    long deleteByExpiresAtBefore(LocalDateTime now);

    @Modifying
    @Transactional
    @Query(
        value = "DELETE FROM refresh_tokens WHERE user_id = :userId",
        nativeQuery = true
    )
    void deleteByUserId(@Param("userId") Long userId);
}
