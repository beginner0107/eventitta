-- V4: Update audit fields based on new BaseEntity hierarchy

-- 1. Festival 테이블에서 created_by, updated_by 컬럼 제거 (BaseTimeEntity로 변경됨)
ALTER TABLE festivals
  DROP COLUMN created_by,
  DROP COLUMN updated_by;

-- 1-2. Festival 테이블에서 제거된 기존 필드들 삭제
ALTER TABLE festivals
  DROP COLUMN host,
  DROP COLUMN supporter,
  DROP COLUMN phone_number,
  DROP COLUMN road_address,
  DROP COLUMN jibun_address,
  DROP COLUMN related_info;

-- 2. meeting_participants 테이블에 BaseTimeEntity 필드 추가
ALTER TABLE meeting_participants
  ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- 3. post_likes 테이블에 BaseTimeEntity 필드 추가하고 기존 liked_at 컬럼 제거
ALTER TABLE post_likes
  ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- 기존 liked_at 데이터를 created_at으로 이전
UPDATE post_likes
SET created_at = liked_at
WHERE liked_at IS NOT NULL;

-- liked_at 컬럼 제거
ALTER TABLE post_likes
  DROP COLUMN liked_at;

-- 4. refresh_tokens 테이블의 BaseTimeEntity 적용
-- 기존 created_at 컬럼을 임시로 백업하고 새로운 audit 필드 추가
ALTER TABLE refresh_tokens
  ADD COLUMN new_created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ADD COLUMN updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- 기존 created_at 데이터를 new_created_at으로 복사
UPDATE refresh_tokens
SET new_created_at = created_at
WHERE created_at IS NOT NULL;

-- 기존 created_at 컬럼 제거하고 new_created_at을 created_at으로 변경
ALTER TABLE refresh_tokens
  DROP COLUMN created_at;

ALTER TABLE refresh_tokens
  CHANGE COLUMN new_created_at created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- 5. 인덱스 정리 및 추가
ALTER TABLE meeting_participants
  ADD INDEX idx_meeting_participants_created_at (created_at);

ALTER TABLE post_likes
  ADD INDEX idx_post_likes_created_at (created_at);

ALTER TABLE refresh_tokens
  ADD INDEX idx_refresh_tokens_created_at (created_at);
