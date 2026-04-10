package com.eventitta.infra.gamification.repository;

import com.eventitta.domain.gamification.domain.ActivityType;
import com.eventitta.domain.gamification.domain.ResourceType;
import com.eventitta.domain.gamification.repository.GamificationActionRecordRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
class JpaGamificationActionRecordRepositoryImpl implements GamificationActionRecordRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public boolean insertGrantIfAbsent(
        Long userId,
        ActivityType activityType,
        ResourceType resourceType,
        Long targetId,
        int pointsEarned
    ) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO user_activities(
                        user_id,
                        activity_type,
                        resource_type,
                        target_id,
                        points_earned,
                        created_at,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                    """,
                userId,
                activityType.name(),
                resourceType.name(),
                targetId,
                pointsEarned
            );
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
