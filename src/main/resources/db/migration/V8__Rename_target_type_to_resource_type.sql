-- V8: Rename target_type to resource_type and update enum values

-- 1. user_activities 테이블의 target_type을 resource_type으로 변경
-- 1-1. 새로운 resource_type 컬럼 추가
ALTER TABLE user_activities
  ADD COLUMN resource_type VARCHAR(50);

-- 1-2. 기존 target_type 데이터를 resource_type으로 매핑하여 복사
-- TargetType -> ResourceType enum 값 매핑
UPDATE user_activities
SET resource_type = CASE target_type
                      WHEN 'POST' THEN 'POST'
                      WHEN 'COMMENT' THEN 'COMMENT'
                      WHEN 'MEETING' THEN 'MEETING'
                      WHEN 'USER' THEN 'SYSTEM'
                      WHEN 'POST_LIKE' THEN 'POST' -- POST_LIKE는 POST로 매핑
                      ELSE 'POST' -- 기본값
  END;

-- 1-3. resource_type을 NOT NULL로 설정
ALTER TABLE user_activities
  MODIFY COLUMN resource_type VARCHAR(50) NOT NULL;

-- 1-4. 기존 target_type 컬럼과 인덱스 삭제
ALTER TABLE user_activities
  DROP INDEX idx_user_activities_target_type,
  DROP COLUMN target_type;

-- 1-5. resource_type 컬럼에 인덱스 추가
ALTER TABLE user_activities
  ADD INDEX idx_user_activities_resource_type (resource_type);

-- 1-6. unique 제약조건 재생성 (target_type -> resource_type 반영)
ALTER TABLE user_activities
  DROP INDEX unique_user_activity_target;

ALTER TABLE user_activities
  ADD UNIQUE KEY unique_user_activity_target (user_id, activity_type, target_id);
