package com.eventitta.infra.auth.repository;

import com.eventitta.domain.auth.domain.AuthActionToken;
import com.eventitta.domain.auth.domain.AuthActionTokenPurpose;
import com.eventitta.domain.auth.repository.AuthActionTokenRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface JpaAuthActionTokenRepository extends JpaRepository<AuthActionToken, Long>, AuthActionTokenRepository {

    Optional<AuthActionToken> findByTokenKey(String tokenKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from AuthActionToken token where token.tokenKey = :tokenKey")
    Optional<AuthActionToken> findByTokenKeyForUpdate(@Param("tokenKey") String tokenKey);

    @Modifying
    @Query("delete from AuthActionToken token where token.userId = :userId and token.purpose = :purpose")
    void deleteByUserIdAndPurpose(@Param("userId") Long userId, @Param("purpose") AuthActionTokenPurpose purpose);

    long deleteByExpiresAtBefore(LocalDateTime now);
}
