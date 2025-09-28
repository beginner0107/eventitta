-- V5: Remove unused festival fields and update gamification domain audit fields

-- 1. Gamification 도메인 테이블들에 BaseTimeEntity 필드 추가

-- activity_types 테이블에 audit 필드 추가
ALTER TABLE activity_types
  ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- badges 테이블에 audit 필드 추가
ALTER TABLE badges
  ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- badge_rules 테이블에 audit 필드 추가
ALTER TABLE badge_rules
  ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- user_activities 테이블에 updated_at 필드만 추가 (created_at과 인덱스는 이미 존재)
ALTER TABLE user_activities
  ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- user_badges 테이블에 updated_at 필드만 추가 (created_at과 인덱스는 이미 존재)
ALTER TABLE user_badges
  ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- 2. 새로 추가된 테이블들에만 인덱스 추가 (기존 테이블 제외)
ALTER TABLE activity_types
  ADD INDEX idx_activity_types_created_at (created_at);

ALTER TABLE badges
  ADD INDEX idx_badges_created_at (created_at);

ALTER TABLE badge_rules
  ADD INDEX idx_badge_rules_created_at (created_at);
