-- Add evaluation_type column to badge_rules table.
-- MySQL does not support PostgreSQL-style COMMENT ON COLUMN syntax,
-- so define the column comment as part of the ALTER TABLE statement.
ALTER TABLE badge_rules
    ADD COLUMN evaluation_type VARCHAR(20) NOT NULL DEFAULT 'COUNT'
    COMMENT 'Badge evaluation type: COUNT (activity count) or POINTS (total points)';

-- Keep existing rows aligned with the non-null default for environments
-- where the column may be backfilled separately before this migration runs.
UPDATE badge_rules
SET evaluation_type = 'COUNT'
WHERE evaluation_type IS NULL;
