package com.eventitta.gamification.repository;

import com.eventitta.gamification.domain.UserPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserPointsRepository extends JpaRepository<UserPoints, Long> {

    Optional<UserPoints> findByUserId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
        insert into user_points (user_id, points, version)
        values (:userId, :delta, 1)
        on duplicate key update points = points + values(points),
                                version = version + 1
        """, nativeQuery = true)
    int upsertAndAddPoints(@Param("userId") Long userId, @Param("delta") int delta);
}
