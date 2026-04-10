ALTER TABLE refresh_tokens
  ADD COLUMN session_id CHAR(36) NULL AFTER user_id,
  ADD COLUMN issued_ip_masked VARCHAR(64) NULL AFTER expires_at,
  ADD COLUMN issued_user_agent VARCHAR(512) NULL AFTER issued_ip_masked,
  ADD COLUMN last_seen_at DATETIME(6) NULL AFTER issued_user_agent,
  ADD COLUMN last_seen_ip_masked VARCHAR(64) NULL AFTER last_seen_at,
  ADD COLUMN last_seen_user_agent VARCHAR(512) NULL AFTER last_seen_ip_masked;

UPDATE refresh_tokens
SET session_id = UUID()
WHERE session_id IS NULL;

UPDATE refresh_tokens
SET last_seen_at = created_at
WHERE last_seen_at IS NULL;

ALTER TABLE refresh_tokens
  MODIFY COLUMN session_id CHAR(36) NOT NULL;

ALTER TABLE refresh_tokens
  ADD CONSTRAINT uk_refresh_tokens_session_id UNIQUE (session_id);

CREATE INDEX idx_refresh_tokens_user_last_seen_at
  ON refresh_tokens (user_id, last_seen_at DESC);
