-- V7: Convert ActivityType from entity to enum and add TargetType

-- 1. user_activities 테이블 구조 변경
-- 1-1. activity_type 컬럼을 String enum으로 변경하기 위해 임시 컬럼 추가
ALTER TABLE user_activities
  ADD COLUMN activity_type_enum VARCHAR(50),
  ADD COLUMN target_type        VARCHAR(50) NOT NULL DEFAULT 'POST';

-- 1-2. 기존 activity_type_id 데이터를 enum 값으로 변환
UPDATE user_activities ua
  INNER JOIN activity_types at ON ua.activity_type_id = at.id
SET ua.activity_type_enum = at.code;

-- 1-3. target_type 값 설정 (ActivityType enum의 targetType과 매핑)
UPDATE user_activities ua
  INNER JOIN activity_types at ON ua.activity_type_id = at.id
SET ua.target_type = CASE at.code
                       WHEN 'CREATE_POST' THEN 'POST'
                       WHEN 'DELETE_POST' THEN 'POST'
                       WHEN 'CREATE_COMMENT' THEN 'COMMENT'
                       WHEN 'DELETE_COMMENT' THEN 'COMMENT'
                       WHEN 'LIKE_POST' THEN 'POST'
                       WHEN 'LIKE_POST_CANCEL' THEN 'POST_LIKE'
                       WHEN 'LIKE_COMMENT' THEN 'COMMENT'
                       WHEN 'LIKE_COMMENT_CANCEL' THEN 'COMMENT'
                       WHEN 'JOIN_MEETING' THEN 'MEETING'
                       WHEN 'JOIN_MEETING_CANCEL' THEN 'MEETING'
                       WHEN 'USER_LOGIN' THEN 'USER'
                       ELSE 'POST'
  END;

-- 1-4. 외래키 제약조건 삭제
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE user_activities
  DROP FOREIGN KEY user_activities_ibfk_2;
SET FOREIGN_KEY_CHECKS = 1;

-- 1-5. 기존 activity_type_id 컬럼과 인덱스 삭제
ALTER TABLE user_activities
  DROP INDEX idx_user_activities_activity_type_id,
  DROP COLUMN activity_type_id;

-- 1-6. activity_type_enum을 activity_type으로 이름 변경하고 NOT NULL 설정
ALTER TABLE user_activities
  CHANGE COLUMN activity_type_enum activity_type VARCHAR(50) NOT NULL;

-- 1-7. target_type 컬럼의 DEFAULT 값 제거
ALTER TABLE user_activities
  ALTER COLUMN target_type DROP DEFAULT;

-- 1-8. 새로운 인덱스 추가
ALTER TABLE user_activities
  ADD INDEX idx_user_activities_activity_type (activity_type),
  ADD INDEX idx_user_activities_target_type (target_type);

-- 2. badge_rules 테이블 구조 변경
-- 2-1. activity_type 컬럼을 String enum으로 변경하기 위해 임시 컬럼 추가
ALTER TABLE badge_rules
  ADD COLUMN activity_type_enum VARCHAR(50);

-- 2-2. 기존 activity_type_id 데이터를 enum 값으로 변환
UPDATE badge_rules br
  INNER JOIN activity_types at ON br.activity_type_id = at.id
SET br.activity_type_enum = at.code;

-- 2-3. 외래키 제약조건 삭제
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE badge_rules
  DROP FOREIGN KEY badge_rules_ibfk_2;
SET FOREIGN_KEY_CHECKS = 1;

-- 2-4. 기존 activity_type_id 컬럼과 인덱스 삭제
ALTER TABLE badge_rules
  DROP INDEX idx_badge_rules_activity_type_id,
  DROP COLUMN activity_type_id;

-- 2-5. activity_type_enum을 activity_type으로 이름 변경하고 NOT NULL 설정
ALTER TABLE badge_rules
  CHANGE COLUMN activity_type_enum activity_type VARCHAR(50) NOT NULL;

-- 2-6. 새로운 인덱스 추가
ALTER TABLE badge_rules
  ADD INDEX idx_badge_rules_activity_type (activity_type);

-- 3. activity_types 테이블 삭제 (더 이상 사용되지 않음)
DROP TABLE IF EXISTS activity_types;

-- 4. 기존 unique 제약조건 업데이트 (user_activities)
-- unique_user_activity_target 제약조건을 새로운 컬럼 구조로 재생성
ALTER TABLE user_activities
  DROP INDEX unique_user_activity_target;

ALTER TABLE user_activities
  ADD UNIQUE KEY unique_user_activity_target (user_id, activity_type, target_id);

-- 5. 기존 unique 제약조건 업데이트 (badge_rules)
-- unique_badge_activity 제약조건을 새로운 컬럼 구조로 재생성
ALTER TABLE badge_rules
  DROP INDEX unique_badge_activity;

ALTER TABLE badge_rules
  ADD UNIQUE KEY unique_badge_activity (badge_id, activity_type);
