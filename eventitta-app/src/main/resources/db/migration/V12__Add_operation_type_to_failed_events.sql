-- V12: Add operation_type column to failed_activity_events table

ALTER TABLE failed_activity_events
    ADD COLUMN operation_type VARCHAR(20) NOT NULL DEFAULT 'RECORD'
    COMMENT '작업 유형(RECORD: 활동 기록, REVOKE: 활동 취소)'
    AFTER activity_type;

-- Add index for operation_type for query optimization
CREATE INDEX idx_failed_event_operation_type ON failed_activity_events(operation_type);
