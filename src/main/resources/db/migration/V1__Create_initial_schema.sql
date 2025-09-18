-- Eventitta 초기 스키마 생성
-- 작성자: System
-- 생성일: 2025-09-17

-- 1. 사용자 관리 테이블
CREATE TABLE users
(
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  email             VARCHAR(255) NOT NULL UNIQUE,
  name              VARCHAR(100) NOT NULL,
  nickname          VARCHAR(50)  NOT NULL UNIQUE,
  password          VARCHAR(255),
  profile_image_url VARCHAR(500),
  provider          VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
  provider_id       VARCHAR(255),
  role              VARCHAR(20)  NOT NULL DEFAULT 'USER',
  points            DECIMAL(10, 0)        DEFAULT 0,
  level_id          BIGINT                DEFAULT 1,
  bio               VARCHAR(500),
  location          VARCHAR(100),
  interests         JSON,
  is_deleted        BOOLEAN               DEFAULT FALSE,
  created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

  INDEX idx_users_email (email),
  INDEX idx_users_nickname (nickname),
  INDEX idx_users_provider_id (provider_id),
  INDEX idx_users_is_deleted (is_deleted)
);

-- 2. 인증 토큰 테이블
CREATE TABLE refresh_tokens
(
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  token      VARCHAR(500) NOT NULL UNIQUE,
  user_id    BIGINT       NOT NULL,
  expires_at DATETIME(6)  NOT NULL,
  created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  INDEX idx_refresh_tokens_token (token),
  INDEX idx_refresh_tokens_user_id (user_id),
  INDEX idx_refresh_tokens_expires_at (expires_at)
);

-- 3. 지역 정보 테이블
CREATE TABLE regions
(
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  code        VARCHAR(20)  NOT NULL UNIQUE,
  parent_id   BIGINT,
  level       INT DEFAULT 1,
  parent_code VARCHAR(20),

  FOREIGN KEY (parent_id) REFERENCES regions (id),
  INDEX idx_regions_code (code),
  INDEX idx_regions_parent_id (parent_id),
  INDEX idx_regions_parent_code (parent_code)
);

-- 4. 축제 정보 테이블
CREATE TABLE festivals
(
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(200) NOT NULL,
  content     TEXT,
  start_date  DATE,
  end_date    DATE,
  location    VARCHAR(200),
  latitude    DECIMAL(10, 8),
  longitude   DECIMAL(11, 8),
  organizer   VARCHAR(100),
  contact     VARCHAR(100),
  homepage    VARCHAR(500),
  image_url   VARCHAR(500),
  data_source VARCHAR(50)  NOT NULL,
  external_id VARCHAR(100),
  region_code VARCHAR(20),
  created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

  INDEX idx_festivals_title (title),
  INDEX idx_festivals_start_date (start_date),
  INDEX idx_festivals_end_date (end_date),
  INDEX idx_festivals_location (location),
  INDEX idx_festivals_data_source (data_source),
  INDEX idx_festivals_external_id (external_id),
  INDEX idx_festivals_region_code (region_code)
);

-- 5. 모임 테이블
CREATE TABLE meetings
(
  id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
  title                VARCHAR(200) NOT NULL,
  description          TEXT,
  meeting_date         DATETIME(6)  NOT NULL,
  location             VARCHAR(200) NOT NULL,
  latitude             DECIMAL(10, 8),
  longitude            DECIMAL(11, 8),
  max_participants     INT          NOT NULL,
  current_participants INT                   DEFAULT 0,
  status               VARCHAR(20)  NOT NULL DEFAULT 'RECRUITING',
  organizer_id         BIGINT       NOT NULL,
  festival_id          BIGINT,
  is_deleted           BOOLEAN               DEFAULT FALSE,
  created_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

  FOREIGN KEY (organizer_id) REFERENCES users (id),
  FOREIGN KEY (festival_id) REFERENCES festivals (id),
  INDEX idx_meetings_title (title),
  INDEX idx_meetings_meeting_date (meeting_date),
  INDEX idx_meetings_location (location),
  INDEX idx_meetings_status (status),
  INDEX idx_meetings_organizer_id (organizer_id),
  INDEX idx_meetings_festival_id (festival_id),
  INDEX idx_meetings_is_deleted (is_deleted)
);

-- 6. 모임 참가자 테이블
CREATE TABLE meeting_participants
(
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  meeting_id BIGINT      NOT NULL,
  user_id    BIGINT      NOT NULL,
  status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  message    VARCHAR(500),
  joined_at  DATETIME(6)          DEFAULT CURRENT_TIMESTAMP(6),

  FOREIGN KEY (meeting_id) REFERENCES meetings (id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  UNIQUE KEY unique_meeting_user (meeting_id, user_id),
  INDEX idx_meeting_participants_meeting_id (meeting_id),
  INDEX idx_meeting_participants_user_id (user_id),
  INDEX idx_meeting_participants_status (status)
);

-- 7. 게시글 테이블
CREATE TABLE posts
(
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(200) NOT NULL,
  content     TEXT         NOT NULL,
  author_id   BIGINT       NOT NULL,
  meeting_id  BIGINT,
  festival_id BIGINT,
  image_urls  JSON,
  like_count  INT                   DEFAULT 0,
  view_count  INT                   DEFAULT 0,
  is_deleted  BOOLEAN               DEFAULT FALSE,
  created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

  FOREIGN KEY (author_id) REFERENCES users (id),
  FOREIGN KEY (meeting_id) REFERENCES meetings (id),
  FOREIGN KEY (festival_id) REFERENCES festivals (id),
  INDEX idx_posts_title (title),
  INDEX idx_posts_author_id (author_id),
  INDEX idx_posts_meeting_id (meeting_id),
  INDEX idx_posts_festival_id (festival_id),
  INDEX idx_posts_created_at (created_at),
  INDEX idx_posts_is_deleted (is_deleted)
);

-- 8. 게시글 좋아요 테이블
CREATE TABLE post_likes
(
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id    BIGINT      NOT NULL,
  user_id    BIGINT      NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

  FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  UNIQUE KEY unique_post_like (post_id, user_id),
  INDEX idx_post_likes_post_id (post_id),
  INDEX idx_post_likes_user_id (user_id)
);

-- 9. 댓글 테이블
CREATE TABLE comments
(
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  content    VARCHAR(1000) NOT NULL,
  author_id  BIGINT        NOT NULL,
  post_id    BIGINT        NOT NULL,
  parent_id  BIGINT,
  is_deleted BOOLEAN                DEFAULT FALSE,
  created_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

  FOREIGN KEY (author_id) REFERENCES users (id),
  FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
  FOREIGN KEY (parent_id) REFERENCES comments (id),
  INDEX idx_comments_post_id (post_id),
  INDEX idx_comments_author_id (author_id),
  INDEX idx_comments_parent_id (parent_id),
  INDEX idx_comments_is_deleted (is_deleted),
  INDEX idx_comments_created_at (created_at)
);

-- 10. 활동 타입 테이블
CREATE TABLE activity_types
(
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  code          VARCHAR(50)  NOT NULL UNIQUE,
  name          VARCHAR(100) NOT NULL,
  default_point INT DEFAULT 0,

  INDEX idx_activity_types_code (code)
);

-- 11. 배지 테이블
CREATE TABLE badges
(
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  name            VARCHAR(100) NOT NULL UNIQUE,
  description     VARCHAR(500),
  icon_url        VARCHAR(500),
  condition_type  VARCHAR(50),
  condition_value INT,
  points          INT                   DEFAULT 0,
  created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

  INDEX idx_badges_name (name),
  INDEX idx_badges_condition_type (condition_type)
);

-- 12. 배지 규칙 테이블
CREATE TABLE badge_rules
(
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  badge_id         BIGINT NOT NULL,
  activity_type_id BIGINT NOT NULL,
  threshold        INT    NOT NULL,
  enabled          BOOLEAN DEFAULT TRUE,

  FOREIGN KEY (badge_id) REFERENCES badges (id) ON DELETE CASCADE,
  FOREIGN KEY (activity_type_id) REFERENCES activity_types (id) ON DELETE CASCADE,
  UNIQUE KEY unique_badge_activity (badge_id, activity_type_id),
  INDEX idx_badge_rules_badge_id (badge_id),
  INDEX idx_badge_rules_activity_type_id (activity_type_id)
);

-- 13. 사용자 배지 테이블
CREATE TABLE user_badges
(
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id   BIGINT      NOT NULL,
  badge_id  BIGINT      NOT NULL,
  earned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  FOREIGN KEY (badge_id) REFERENCES badges (id) ON DELETE CASCADE,
  UNIQUE KEY unique_user_badge (user_id, badge_id),
  INDEX idx_user_badges_user_id (user_id),
  INDEX idx_user_badges_badge_id (badge_id)
);

-- 14. 사용자 활동 테이블
CREATE TABLE user_activities
(
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id       BIGINT      NOT NULL,
  activity_type VARCHAR(50) NOT NULL,
  target_id     BIGINT,
  points        INT                  DEFAULT 0,
  description   VARCHAR(200),
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  INDEX idx_user_activities_user_id (user_id),
  INDEX idx_user_activities_activity_type (activity_type),
  INDEX idx_user_activities_created_at (created_at)
);

-- 15. ShedLock 테이블 (스케줄링 잠금)
CREATE TABLE shedlock
(
  name       VARCHAR(64)  NOT NULL PRIMARY KEY,
  lock_until TIMESTAMP    NOT NULL,
  locked_at  TIMESTAMP    NOT NULL,
  locked_by  VARCHAR(255) NOT NULL
);
