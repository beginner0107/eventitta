CREATE TABLE media_asset
(
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_user_id     BIGINT        NOT NULL,
  category          VARCHAR(30)   NOT NULL,
  storage_provider  VARCHAR(30)   NOT NULL,
  storage_key       VARCHAR(1024) NOT NULL,
  public_url        VARCHAR(1024) NOT NULL,
  original_filename VARCHAR(255)  NOT NULL,
  content_type      VARCHAR(100)  NOT NULL,
  size_bytes        BIGINT        NOT NULL DEFAULT 0,
  checksum          VARCHAR(64),
  width             INT,
  height            INT,
  status            VARCHAR(30)   NOT NULL,
  attached_at       DATETIME(6),
  deleted_at        DATETIME(6),
  created_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by        VARCHAR(100),
  updated_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by        VARCHAR(100),
  legacy_post_image_id BIGINT,
  legacy_user_id    BIGINT,

  CONSTRAINT fk_media_asset_owner_user FOREIGN KEY (owner_user_id) REFERENCES users (id),
  INDEX idx_media_asset_owner_status (owner_user_id, status),
  INDEX idx_media_asset_status_created_at (status, created_at),
  INDEX idx_media_asset_status_updated_at (status, updated_at),
  INDEX idx_media_asset_storage_key (storage_key(191))
);

ALTER TABLE post_image
  ADD COLUMN media_asset_id BIGINT NULL,
  ADD CONSTRAINT fk_post_image_media_asset FOREIGN KEY (media_asset_id) REFERENCES media_asset (id);

ALTER TABLE users
  ADD COLUMN profile_picture_media_id BIGINT NULL,
  ADD CONSTRAINT fk_users_profile_picture_media FOREIGN KEY (profile_picture_media_id) REFERENCES media_asset (id);

INSERT INTO media_asset (
  owner_user_id,
  category,
  storage_provider,
  storage_key,
  public_url,
  original_filename,
  content_type,
  size_bytes,
  status,
  attached_at,
  created_at,
  created_by,
  updated_at,
  updated_by,
  legacy_post_image_id
)
SELECT
  p.user_id,
  'POST_IMAGE',
  'LEGACY',
  CASE
    WHEN LOCATE('/api/v1/uploads/', pi.image_url) > 0 THEN SUBSTRING(pi.image_url, LOCATE('/api/v1/uploads/', pi.image_url) + LENGTH('/api/v1/uploads/'))
    WHEN LOCATE('/uploads/', pi.image_url) > 0 THEN SUBSTRING(pi.image_url, LOCATE('/uploads/', pi.image_url) + LENGTH('/uploads/'))
    ELSE TRIM(LEADING '/' FROM pi.image_url)
  END,
  CASE
    WHEN pi.image_url LIKE 'http%' OR pi.image_url LIKE '/api/v1/uploads/%' THEN pi.image_url
    ELSE CONCAT('/api/v1/uploads/',
      CASE
        WHEN LOCATE('/uploads/', pi.image_url) > 0 THEN SUBSTRING(pi.image_url, LOCATE('/uploads/', pi.image_url) + LENGTH('/uploads/'))
        ELSE TRIM(LEADING '/' FROM pi.image_url)
      END
    )
  END,
  SUBSTRING_INDEX(
    CASE
      WHEN LOCATE('/api/v1/uploads/', pi.image_url) > 0 THEN SUBSTRING(pi.image_url, LOCATE('/api/v1/uploads/', pi.image_url) + LENGTH('/api/v1/uploads/'))
      WHEN LOCATE('/uploads/', pi.image_url) > 0 THEN SUBSTRING(pi.image_url, LOCATE('/uploads/', pi.image_url) + LENGTH('/uploads/'))
      ELSE TRIM(LEADING '/' FROM pi.image_url)
    END,
    '/',
    -1
  ),
  'application/octet-stream',
  0,
  'ATTACHED',
  pi.created_at,
  pi.created_at,
  pi.created_by,
  pi.updated_at,
  pi.updated_by,
  pi.id
FROM post_image pi
JOIN posts p ON p.id = pi.post_id;

UPDATE post_image pi
JOIN media_asset ma ON ma.legacy_post_image_id = pi.id
SET pi.media_asset_id = ma.id;

INSERT INTO media_asset (
  owner_user_id,
  category,
  storage_provider,
  storage_key,
  public_url,
  original_filename,
  content_type,
  size_bytes,
  status,
  attached_at,
  created_at,
  created_by,
  updated_at,
  updated_by,
  legacy_user_id
)
SELECT
  u.id,
  'PROFILE_IMAGE',
  'LEGACY',
  CASE
    WHEN LOCATE('/api/v1/uploads/', u.profile_picture_url) > 0 THEN SUBSTRING(u.profile_picture_url, LOCATE('/api/v1/uploads/', u.profile_picture_url) + LENGTH('/api/v1/uploads/'))
    WHEN LOCATE('/uploads/', u.profile_picture_url) > 0 THEN SUBSTRING(u.profile_picture_url, LOCATE('/uploads/', u.profile_picture_url) + LENGTH('/uploads/'))
    ELSE TRIM(LEADING '/' FROM u.profile_picture_url)
  END,
  CASE
    WHEN u.profile_picture_url LIKE 'http%' OR u.profile_picture_url LIKE '/api/v1/uploads/%' THEN u.profile_picture_url
    ELSE CONCAT('/api/v1/uploads/',
      CASE
        WHEN LOCATE('/uploads/', u.profile_picture_url) > 0 THEN SUBSTRING(u.profile_picture_url, LOCATE('/uploads/', u.profile_picture_url) + LENGTH('/uploads/'))
        ELSE TRIM(LEADING '/' FROM u.profile_picture_url)
      END
    )
  END,
  SUBSTRING_INDEX(
    CASE
      WHEN LOCATE('/api/v1/uploads/', u.profile_picture_url) > 0 THEN SUBSTRING(u.profile_picture_url, LOCATE('/api/v1/uploads/', u.profile_picture_url) + LENGTH('/api/v1/uploads/'))
      WHEN LOCATE('/uploads/', u.profile_picture_url) > 0 THEN SUBSTRING(u.profile_picture_url, LOCATE('/uploads/', u.profile_picture_url) + LENGTH('/uploads/'))
      ELSE TRIM(LEADING '/' FROM u.profile_picture_url)
    END,
    '/',
    -1
  ),
  'application/octet-stream',
  0,
  'ATTACHED',
  u.updated_at,
  u.created_at,
  u.created_by,
  u.updated_at,
  u.updated_by,
  u.id
FROM users u
WHERE u.profile_picture_url IS NOT NULL
  AND u.profile_picture_url <> '';

UPDATE users u
JOIN media_asset ma ON ma.legacy_user_id = u.id AND ma.category = 'PROFILE_IMAGE'
SET u.profile_picture_media_id = ma.id
WHERE u.profile_picture_url IS NOT NULL
  AND u.profile_picture_url <> '';

ALTER TABLE post_image
  MODIFY COLUMN media_asset_id BIGINT NOT NULL;

ALTER TABLE media_asset
  DROP COLUMN legacy_post_image_id,
  DROP COLUMN legacy_user_id;
