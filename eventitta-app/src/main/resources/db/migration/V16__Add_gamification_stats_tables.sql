CREATE TABLE user_gamification_stats
(
  user_id              BIGINT      NOT NULL PRIMARY KEY,
  total_points         INT         NOT NULL DEFAULT 0,
  total_activity_count BIGINT      NOT NULL DEFAULT 0,
  updated_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

  CONSTRAINT fk_user_gamification_stats_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  INDEX idx_user_gamification_stats_total_points (total_points),
  INDEX idx_user_gamification_stats_total_activity_count (total_activity_count)
);

CREATE TABLE user_activity_stats
(
  user_id      BIGINT      NOT NULL,
  action_type  VARCHAR(50) NOT NULL,
  action_count BIGINT      NOT NULL DEFAULT 0,
  points_total INT         NOT NULL DEFAULT 0,
  updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

  PRIMARY KEY (user_id, action_type),
  CONSTRAINT fk_user_activity_stats_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  INDEX idx_user_activity_stats_action_type (action_type)
);

INSERT INTO user_gamification_stats (user_id, total_points, total_activity_count, updated_at)
SELECT u.id,
       u.points,
       COALESCE(ua.activity_count, 0),
       CURRENT_TIMESTAMP(6)
FROM users u
LEFT JOIN (
  SELECT user_id, COUNT(*) AS activity_count
  FROM user_activities
  WHERE activity_type IN ('CREATE_POST', 'CREATE_COMMENT', 'LIKE_POST', 'JOIN_MEETING')
  GROUP BY user_id
) ua ON ua.user_id = u.id;

INSERT INTO user_activity_stats (user_id, action_type, action_count, points_total, updated_at)
SELECT user_id,
       activity_type,
       COUNT(*)          AS action_count,
       SUM(points_earned) AS points_total,
       CURRENT_TIMESTAMP(6)
FROM user_activities
WHERE activity_type IN ('CREATE_POST', 'CREATE_COMMENT', 'LIKE_POST', 'JOIN_MEETING')
GROUP BY user_id, activity_type;
