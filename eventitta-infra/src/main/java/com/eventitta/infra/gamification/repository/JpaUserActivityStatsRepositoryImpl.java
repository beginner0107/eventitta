package com.eventitta.infra.gamification.repository;

import com.eventitta.domain.gamification.domain.RewardActionType;
import com.eventitta.domain.gamification.repository.UserActivityStatsRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
class JpaUserActivityStatsRepositoryImpl implements UserActivityStatsRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void increment(Long userId, RewardActionType actionType, int pointsDelta) {
        int updated = jdbcTemplate.update("""
                UPDATE user_activity_stats
                SET action_count = action_count + 1,
                    points_total = points_total + ?,
                    updated_at = CURRENT_TIMESTAMP()
                WHERE user_id = ?
                  AND action_type = ?
                """,
            pointsDelta,
            userId,
            actionType.name()
        );

        if (updated > 0) {
            return;
        }

        try {
            jdbcTemplate.update("""
                    INSERT INTO user_activity_stats(user_id, action_type, action_count, points_total, updated_at)
                    VALUES (?, ?, 1, ?, CURRENT_TIMESTAMP())
                    """,
                userId,
                actionType.name(),
                pointsDelta
            );
        } catch (DuplicateKeyException e) {
            jdbcTemplate.update("""
                    UPDATE user_activity_stats
                    SET action_count = action_count + 1,
                        points_total = points_total + ?,
                        updated_at = CURRENT_TIMESTAMP()
                    WHERE user_id = ?
                      AND action_type = ?
                    """,
                pointsDelta,
                userId,
                actionType.name()
            );
        }
    }

    @Override
    @Transactional
    public void decrement(Long userId, RewardActionType actionType, int pointsDelta) {
        jdbcTemplate.update("""
                UPDATE user_activity_stats
                SET action_count = CASE
                                       WHEN action_count > 0 THEN action_count - 1
                                       ELSE 0
                    END,
                    points_total = CASE
                                       WHEN points_total >= ? THEN points_total - ?
                                       ELSE 0
                    END,
                    updated_at = CURRENT_TIMESTAMP()
                WHERE user_id = ?
                  AND action_type = ?
                """,
            pointsDelta,
            pointsDelta,
            userId,
            actionType.name()
        );

        jdbcTemplate.update("""
                DELETE FROM user_activity_stats
                WHERE user_id = ?
                  AND action_type = ?
                  AND action_count = 0
                  AND points_total = 0
                """,
            userId,
            actionType.name()
        );
    }
}
