-- Eventitta 초기 스키마 생성
-- 작성자: System
-- 생성일: 2025-09-17

-- 1. 사용자 관리 테이블
CREATE TABLE users
(
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  email               VARCHAR(255) NOT NULL UNIQUE,
  password            VARCHAR(255) NOT NULL,
  nickname            VARCHAR(100) NOT NULL,
  profile_picture_url VARCHAR(512),
  self_intro          TEXT,
  interests           JSON,
  address             VARCHAR(255),
  latitude            DECIMAL(9, 6),
  longitude           DECIMAL(9, 6),
  role                VARCHAR(50)  NOT NULL,
  provider            VARCHAR(50)  NOT NULL,
  provider_id         VARCHAR(100),
  deleted             BOOLEAN               DEFAULT FALSE,
  created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by          VARCHAR(100),
  updated_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by          VARCHAR(100),

  INDEX idx_users_email (email),
  INDEX idx_users_nickname (nickname),
  INDEX idx_users_provider_id (provider_id),
  INDEX idx_users_deleted (deleted)
);

-- 2. 인증 토큰 테이블
CREATE TABLE refresh_tokens
(
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  token_hash VARCHAR(255) NOT NULL,
  user_id    BIGINT       NOT NULL,
  created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  expires_at DATETIME(6)  NOT NULL,

  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  INDEX idx_refresh_tokens_token_hash (token_hash),
  INDEX idx_refresh_tokens_user_id (user_id),
  INDEX idx_refresh_tokens_expires_at (expires_at)
);

-- 3. 지역 정보 테이블
CREATE TABLE regions
(
  code        VARCHAR(20) PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  parent_code VARCHAR(20),
  level       INT,

  INDEX idx_regions_parent_code (parent_code)
);

-- 4. 축제 정보 테이블
CREATE TABLE festivals
(
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  title           VARCHAR(1000) NOT NULL,
  venue           VARCHAR(1000),
  start_date      DATE,
  end_date        DATE,
  category        VARCHAR(100),
  district        VARCHAR(100),
  target_audience VARCHAR(500),
  fee_info        VARCHAR(1000),
  is_free         BOOLEAN,
  performers      TEXT,
  program_info    TEXT,
  main_image_url  VARCHAR(1000),
  theme_code      VARCHAR(100),
  ticket_type     VARCHAR(50),
  organizer       VARCHAR(500),
  host            VARCHAR(500),
  supporter       VARCHAR(500),
  phone_number    VARCHAR(50),
  homepage_url    VARCHAR(1000),
  detail_url      VARCHAR(1000),
  road_address    VARCHAR(500),
  jibun_address   VARCHAR(500),
  latitude        DECIMAL(10, 7),
  longitude       DECIMAL(10, 7),
  content         TEXT,
  related_info    TEXT,
  data_source     VARCHAR(50)   NOT NULL,
  external_id     VARCHAR(100)  NOT NULL,
  created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by      VARCHAR(100),
  updated_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by      VARCHAR(100),

  UNIQUE KEY unique_external_data_source (external_id, data_source),
  INDEX idx_festivals_title (title(100)),
  INDEX idx_festivals_start_date (start_date),
  INDEX idx_festivals_end_date (end_date),
  INDEX idx_festivals_venue (venue(100)),
  INDEX idx_festivals_data_source (data_source),
  INDEX idx_festivals_location (latitude, longitude)
);

-- 5. 모임 테이블
CREATE TABLE meetings
(
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  title           VARCHAR(200) NOT NULL,
  description     TEXT,
  start_time      DATETIME(6)  NOT NULL,
  end_time        DATETIME(6)  NOT NULL,
  max_members     INT          NOT NULL,
  current_members INT                   DEFAULT 1,
  address         VARCHAR(200) NOT NULL,
  latitude        DOUBLE,
  longitude       DOUBLE,
  status          VARCHAR(20)  NOT NULL,
  leader_id       BIGINT       NOT NULL,
  deleted         BOOLEAN               DEFAULT FALSE,
  created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by      VARCHAR(100),
  updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by      VARCHAR(100),

  FOREIGN KEY (leader_id) REFERENCES users (id),
  INDEX idx_meetings_title (title),
  INDEX idx_meetings_start_time (start_time),
  INDEX idx_meetings_address (address),
  INDEX idx_meetings_status (status),
  INDEX idx_meetings_leader_id (leader_id),
  INDEX idx_meetings_deleted (deleted)
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
  user_id     BIGINT       NOT NULL,
  title       VARCHAR(255) NOT NULL,
  content     LONGTEXT     NOT NULL,
  region_code VARCHAR(20)  NOT NULL,
  like_count  INT                   DEFAULT 0,
  deleted     BOOLEAN               DEFAULT FALSE,
  created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by  VARCHAR(100),
  updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by  VARCHAR(100),

  FOREIGN KEY (user_id) REFERENCES users (id),
  FOREIGN KEY (region_code) REFERENCES regions (code),
  INDEX idx_posts_user_id (user_id),
  INDEX idx_posts_region_code (region_code),
  INDEX idx_posts_title (title),
  INDEX idx_posts_created_at (created_at),
  INDEX idx_posts_deleted (deleted)
);

-- 8. 게시글 이미지 테이블
CREATE TABLE post_image
(
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id    BIGINT       NOT NULL,
  image_url  VARCHAR(500) NOT NULL,
  sort_order INT          NOT NULL DEFAULT 0,
  created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by VARCHAR(100),
  updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(100),

  FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
  INDEX idx_post_image_post_id (post_id)
);

-- 9. 게시글 좋아요 테이블
CREATE TABLE post_likes
(
  id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id  BIGINT      NOT NULL,
  user_id  BIGINT      NOT NULL,
  liked_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

  FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  UNIQUE KEY unique_post_like (post_id, user_id),
  INDEX idx_post_likes_post_id (post_id),
  INDEX idx_post_likes_user_id (user_id)
);

-- 10. 댓글 테이블
CREATE TABLE comments
(
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id           BIGINT        NOT NULL,
  user_id           BIGINT        NOT NULL,
  content           VARCHAR(1000) NOT NULL,
  parent_comment_id BIGINT,
  deleted           BOOLEAN                DEFAULT FALSE,
  created_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by        VARCHAR(100),
  updated_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by        VARCHAR(100),

  FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users (id),
  FOREIGN KEY (parent_comment_id) REFERENCES comments (id),
  INDEX idx_comments_post_id (post_id),
  INDEX idx_comments_user_id (user_id),
  INDEX idx_comments_parent_comment_id (parent_comment_id),
  INDEX idx_comments_deleted (deleted),
  INDEX idx_comments_created_at (created_at)
);

-- 11. 활동 타입 테이블
CREATE TABLE activity_types
(
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  code          VARCHAR(50)  NOT NULL UNIQUE,
  name          VARCHAR(100) NOT NULL,
  default_point INT          NOT NULL,

  INDEX idx_activity_types_code (code)
);

-- 12. 배지 테이블
CREATE TABLE badges
(
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL UNIQUE,
  description VARCHAR(500) NOT NULL,
  icon_url    VARCHAR(500),

  INDEX idx_badges_name (name)
);

-- 13. 배지 규칙 테이블
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

-- 14. 사용자 배지 테이블
CREATE TABLE user_badges
(
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT      NOT NULL,
  badge_id   BIGINT      NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  FOREIGN KEY (badge_id) REFERENCES badges (id) ON DELETE CASCADE,
  UNIQUE KEY unique_user_badge (user_id, badge_id),
  INDEX idx_user_badges_user_id (user_id),
  INDEX idx_user_badges_badge_id (badge_id)
);

-- 15. 사용자 활동 테이블
CREATE TABLE user_activities
(
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id          BIGINT      NOT NULL,
  activity_type_id BIGINT      NOT NULL,
  points_earned    INT         NOT NULL,
  target_id        BIGINT      NOT NULL,
  created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version          BIGINT      NOT NULL DEFAULT 0,

  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  FOREIGN KEY (activity_type_id) REFERENCES activity_types (id) ON DELETE CASCADE,
  UNIQUE KEY unique_user_activity_target (user_id, activity_type_id, target_id),
  INDEX idx_user_activities_user_id (user_id),
  INDEX idx_user_activities_activity_type_id (activity_type_id),
  INDEX idx_user_activities_created_at (created_at)
);

-- 16. 사용자 포인트 테이블
CREATE TABLE user_points
(
  user_id BIGINT NOT NULL PRIMARY KEY,
  points  INT    NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0,

  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 17. ShedLock 테이블 (스케줄링 잠금)
CREATE TABLE shedlock
(
  name       VARCHAR(64)  NOT NULL PRIMARY KEY,
  lock_until TIMESTAMP    NOT NULL,
  locked_at  TIMESTAMP    NOT NULL,
  locked_by  VARCHAR(255) NOT NULL
);
