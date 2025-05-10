package com.eventitta.user.repository;

import com.eventitta.user.domain.User;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findByEmail(String email);
}
