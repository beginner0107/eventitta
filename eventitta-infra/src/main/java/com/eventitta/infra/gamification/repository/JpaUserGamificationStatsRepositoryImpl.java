package com.eventitta.infra.gamification.repository;

import com.eventitta.domain.gamification.repository.UserGamificationStatsRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
class JpaUserGamificationStatsRepositoryImpl implements UserGamificationStatsRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void increment(Long userId, int pointsDelta, long activityCountDelta) {
        int updated = jdbcTemplate.update("""
                UPDATE user_gamification_stats
                SET total_points = total_points + ?,
                    total_activity_count = total_activity_count + ?,
                    updated_at = CURRENT_TIMESTAMP()
                WHERE user_id = ?
                """,
            pointsDelta,
            activityCountDelta,
            userId
        );

        if (updated > 0) {
            return;
        }

        try {
            jdbcTemplate.update("""
                    INSERT INTO user_gamification_stats(user_id, total_points, total_activity_count, updated_at)
                    VALUES (?, ?, ?, CURRENT_TIMESTAMP())
                    """,
                userId,
                pointsDelta,
                activityCountDelta
            );
        } catch (DuplicateKeyException e) {
            jdbcTemplate.update("""
                    UPDATE user_gamification_stats
                    SET total_points = total_points + ?,
                        total_activity_count = total_activity_count + ?,
                        updated_at = CURRENT_TIMESTAMP()
                    WHERE user_id = ?
                    """,
                pointsDelta,
                activityCountDelta,
                userId
            );
        }
    }

    @Override
    @Transactional
    public void decrement(Long userId, int pointsDelta, long activityCountDelta) {
        jdbcTemplate.update("""
                UPDATE user_gamification_stats
                SET total_points = CASE
                                       WHEN total_points >= ? THEN total_points - ?
                                       ELSE 0
                    END,
                    total_activity_count = CASE
                                               WHEN total_activity_count >= ? THEN total_activity_count - ?
                                               ELSE 0
                    END,
                    updated_at = CURRENT_TIMESTAMP()
                WHERE user_id = ?
                """,
            pointsDelta,
            pointsDelta,
            activityCountDelta,
            activityCountDelta,
            userId
        );
    }
}
