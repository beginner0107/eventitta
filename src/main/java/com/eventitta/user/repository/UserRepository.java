package com.eventitta.user.repository;

import com.eventitta.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * 포인트 순위 Top N 조회 (MySQL fallback용)
     *
     * @param pageable 페이징 정보
     * @return 포인트가 높은 순으로 정렬된 유저 목록
     */
    @Query("SELECT u FROM User u " +
        "WHERE u.deleted = false " +
        "ORDER BY u.points DESC")
    List<User> findTopUsersByPoints(org.springframework.data.domain.Pageable pageable);

    /**
     * 특정 포인트보다 높은 유저 수 조회 (순위 계산용)
     *
     * @param points 비교할 포인트
     * @return 해당 포인트보다 높은 유저 수
     */
    @Query("SELECT COUNT(u) FROM User u " +
        "WHERE u.deleted = false AND u.points > :points")
    long countByPointsGreaterThan(@Param("points") int points);
}
