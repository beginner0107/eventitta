-- Add evaluation_type column to badge_rules table
ALTER TABLE badge_rules ADD COLUMN evaluation_type VARCHAR(20) NOT NULL DEFAULT 'COUNT';

-- Update existing records to use COUNT as default
UPDATE badge_rules SET evaluation_type = 'COUNT' WHERE evaluation_type IS NULL;

-- Add comment for clarity
COMMENT ON COLUMN badge_rules.evaluation_type IS 'Badge evaluation type: COUNT (activity count) or POINTS (total points)';
