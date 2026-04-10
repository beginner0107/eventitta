ALTER TABLE users
  ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE AFTER password,
  ADD COLUMN suspended BOOLEAN NOT NULL DEFAULT FALSE AFTER deleted,
  ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0 AFTER suspended;

ALTER TABLE auth_identity
  ADD CONSTRAINT uk_auth_identity_user_provider UNIQUE (user_id, provider);

CREATE TABLE auth_action_tokens
(
  id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id                BIGINT       NOT NULL,
  token_purpose          VARCHAR(50)  NOT NULL,
  token_key              VARCHAR(100) NOT NULL,
  token_hash             VARCHAR(255) NOT NULL,
  target_email           VARCHAR(255),
  expires_at             DATETIME(6)  NOT NULL,
  used_at                DATETIME(6),
  requested_ip_masked    VARCHAR(64),
  requested_user_agent   VARCHAR(512),
  created_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by             VARCHAR(100),
  updated_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by             VARCHAR(100),

  CONSTRAINT fk_auth_action_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT uk_auth_action_tokens_token_key UNIQUE (token_key),
  INDEX idx_auth_action_tokens_user_purpose (user_id, token_purpose),
  INDEX idx_auth_action_tokens_expires_at (expires_at)
);

CREATE TABLE auth_security_events
(
  id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id              BIGINT,
  event_type           VARCHAR(50)   NOT NULL,
  event_outcome        VARCHAR(30)   NOT NULL,
  session_id           VARCHAR(36),
  identifier           VARCHAR(255),
  client_ip_masked     VARCHAR(64),
  client_user_agent    VARCHAR(512),
  detail_message       VARCHAR(1000),
  created_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

  INDEX idx_auth_security_events_user_created_at (user_id, created_at DESC),
  INDEX idx_auth_security_events_type_created_at (event_type, created_at DESC)
);
