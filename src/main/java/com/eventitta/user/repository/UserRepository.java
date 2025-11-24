package com.eventitta.user.repository;

import com.eventitta.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findByEmail(String email);

    @Query("select u from User u where u.email = :email and u.deleted = false")
    Optional<User> findActiveByEmail(@Param("email") String email);

    @Query("select u from User u where u.id = :id and u.deleted = false")
    Optional<User> findActiveById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.points = u.points + :amount " +
        "WHERE u.id = :userId")
    int incrementPoints(@Param("userId") Long userId, @Param("amount") int amount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.points = u.points - :amount " +
        "WHERE u.id = :userId AND u.points >= :amount")
    int decrementPoints(@Param("userId") Long userId, @Param("amount") int amount);
}
