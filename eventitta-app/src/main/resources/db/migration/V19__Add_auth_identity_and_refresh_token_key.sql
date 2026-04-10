DELETE FROM refresh_tokens;

ALTER TABLE refresh_tokens
  ADD COLUMN token_key VARCHAR(100) NOT NULL AFTER user_id;

ALTER TABLE refresh_tokens
  ADD CONSTRAINT uk_refresh_tokens_token_key UNIQUE (token_key);

CREATE TABLE auth_identity
(
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id          BIGINT       NOT NULL,
  provider         VARCHAR(50)  NOT NULL,
  provider_user_id VARCHAR(100) NOT NULL,
  provider_email   VARCHAR(255),
  email_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by       VARCHAR(100),
  updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by       VARCHAR(100),

  CONSTRAINT fk_auth_identity_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT uk_auth_identity_provider_user_id UNIQUE (provider, provider_user_id),
  INDEX idx_auth_identity_user_id (user_id)
);
