ALTER TABLE media_asset
  ADD COLUMN processing_status VARCHAR(30) NOT NULL DEFAULT 'READY' AFTER status,
  ADD COLUMN processing_error VARCHAR(500) NULL AFTER processing_status;

CREATE TABLE media_asset_variant
(
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  media_asset_id BIGINT        NOT NULL,
  variant_type  VARCHAR(30)   NOT NULL,
  storage_key   VARCHAR(1024) NOT NULL,
  public_url    VARCHAR(1024) NOT NULL,
  format        VARCHAR(20)   NOT NULL,
  width         INT,
  height        INT,
  size_bytes    BIGINT        NOT NULL DEFAULT 0,
  status        VARCHAR(30)   NOT NULL,
  created_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by    VARCHAR(100),
  updated_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by    VARCHAR(100),

  CONSTRAINT fk_media_asset_variant_asset FOREIGN KEY (media_asset_id) REFERENCES media_asset (id),
  CONSTRAINT uk_media_asset_variant_type UNIQUE (media_asset_id, variant_type),
  INDEX idx_media_asset_variant_asset_status (media_asset_id, status)
);

INSERT INTO media_asset_variant (
  media_asset_id,
  variant_type,
  storage_key,
  public_url,
  format,
  width,
  height,
  size_bytes,
  status,
  created_at,
  created_by,
  updated_at,
  updated_by
)
SELECT
  ma.id,
  'DETAIL',
  ma.storage_key,
  ma.public_url,
  LOWER(SUBSTRING_INDEX(ma.original_filename, '.', -1)),
  ma.width,
  ma.height,
  ma.size_bytes,
  'READY',
  ma.created_at,
  ma.created_by,
  ma.updated_at,
  ma.updated_by
FROM media_asset ma
WHERE ma.category = 'POST_IMAGE'
  AND ma.public_url IS NOT NULL
  AND ma.public_url <> '';

INSERT INTO media_asset_variant (
  media_asset_id,
  variant_type,
  storage_key,
  public_url,
  format,
  width,
  height,
  size_bytes,
  status,
  created_at,
  created_by,
  updated_at,
  updated_by
)
SELECT
  ma.id,
  'THUMB',
  ma.storage_key,
  ma.public_url,
  LOWER(SUBSTRING_INDEX(ma.original_filename, '.', -1)),
  ma.width,
  ma.height,
  ma.size_bytes,
  'READY',
  ma.created_at,
  ma.created_by,
  ma.updated_at,
  ma.updated_by
FROM media_asset ma
WHERE ma.category = 'POST_IMAGE'
  AND ma.public_url IS NOT NULL
  AND ma.public_url <> '';

INSERT INTO media_asset_variant (
  media_asset_id,
  variant_type,
  storage_key,
  public_url,
  format,
  width,
  height,
  size_bytes,
  status,
  created_at,
  created_by,
  updated_at,
  updated_by
)
SELECT
  ma.id,
  'AVATAR',
  ma.storage_key,
  ma.public_url,
  LOWER(SUBSTRING_INDEX(ma.original_filename, '.', -1)),
  ma.width,
  ma.height,
  ma.size_bytes,
  'READY',
  ma.created_at,
  ma.created_by,
  ma.updated_at,
  ma.updated_by
FROM media_asset ma
WHERE ma.category = 'PROFILE_IMAGE'
  AND ma.public_url IS NOT NULL
  AND ma.public_url <> '';
