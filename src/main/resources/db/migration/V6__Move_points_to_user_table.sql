-- V6: Move points from user_points table to users table

-- 1. users 테이블에 points 컬럼 추가
ALTER TABLE users
  ADD COLUMN points INT NOT NULL DEFAULT 0;

-- 2. 기존 user_points 데이터를 users 테이블로 이동
UPDATE users u
  INNER JOIN user_points up ON u.id = up.user_id
SET u.points = up.points;

-- 3. user_points 테이블의 외래키 제약조건 삭제 (있는 경우)
SET FOREIGN_KEY_CHECKS = 0;

-- 4. user_points 테이블 삭제
DROP TABLE IF EXISTS user_points;

-- 5. 외래키 체크 재활성화
SET FOREIGN_KEY_CHECKS = 1;

-- 6. users.points 컬럼에 인덱스 추가 (성능 최적화)
ALTER TABLE users
  ADD INDEX idx_users_points (points);
