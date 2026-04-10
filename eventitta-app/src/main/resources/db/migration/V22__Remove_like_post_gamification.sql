-- V22: Remove post-like gamification contributions and rebuild derived stats

-- 1. Remove any queued or failed LIKE_POST processing traces.
DELETE FROM activity_outbox
WHERE activity_type = 'LIKE_POST';

DELETE FROM failed_activity_events
WHERE activity_type = 'LIKE_POST';

-- 2. Remove LIKE_POST grants and related badge history.
DELETE FROM user_activities
WHERE activity_type = 'LIKE_POST';

DELETE ub
FROM user_badges ub
JOIN badge_rules br ON br.badge_id = ub.badge_id
WHERE br.activity_type = 'LIKE_POST';

DELETE FROM badge_rules
WHERE activity_type = 'LIKE_POST';

DELETE b
FROM badges b
LEFT JOIN badge_rules br ON br.badge_id = b.id
WHERE b.name = '프로 좋아요꾼'
  AND br.id IS NULL;

-- 3. Rebuild per-action stats from the remaining rewarded activities.
DELETE FROM user_activity_stats;

INSERT INTO user_activity_stats (user_id, action_type, action_count, points_total, updated_at)
SELECT ua.user_id,
       ua.activity_type,
       COUNT(*)           AS action_count,
       COALESCE(SUM(ua.points_earned), 0) AS points_total,
       CURRENT_TIMESTAMP(6)
FROM user_activities ua
WHERE ua.activity_type IN ('CREATE_POST', 'CREATE_COMMENT', 'JOIN_MEETING')
GROUP BY ua.user_id, ua.activity_type;

-- 4. Rebuild aggregate stats and keep users.points in sync for legacy readers.
INSERT INTO user_gamification_stats (user_id, total_points, total_activity_count, updated_at)
SELECT u.id,
       COALESCE(agg.total_points, 0)         AS total_points,
       COALESCE(agg.total_activity_count, 0) AS total_activity_count,
       CURRENT_TIMESTAMP(6)
FROM users u
LEFT JOIN (
    SELECT ua.user_id,
           COALESCE(SUM(ua.points_earned), 0) AS total_points,
           COUNT(*)                           AS total_activity_count
    FROM user_activities ua
    WHERE ua.activity_type IN ('CREATE_POST', 'CREATE_COMMENT', 'JOIN_MEETING')
    GROUP BY ua.user_id
) agg ON agg.user_id = u.id
ON DUPLICATE KEY UPDATE total_points = VALUES(total_points),
                        total_activity_count = VALUES(total_activity_count),
                        updated_at = VALUES(updated_at);

UPDATE users u
LEFT JOIN (
    SELECT ua.user_id,
           COALESCE(SUM(ua.points_earned), 0) AS total_points
    FROM user_activities ua
    WHERE ua.activity_type IN ('CREATE_POST', 'CREATE_COMMENT', 'JOIN_MEETING')
    GROUP BY ua.user_id
) agg ON agg.user_id = u.id
SET u.points = COALESCE(agg.total_points, 0);
