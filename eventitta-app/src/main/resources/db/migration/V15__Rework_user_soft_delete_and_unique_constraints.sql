ALTER TABLE users
  MODIFY password VARCHAR(255) NULL;

UPDATE users
SET nickname = CONCAT('__deleted_user_', id, '_', REPLACE(UUID(), '-', '')),
    email = CONCAT('__deleted_user_', id, '_', REPLACE(UUID(), '-', ''), '@deleted.local')
WHERE deleted = TRUE;

ALTER TABLE users
  DROP INDEX email;

ALTER TABLE users
  ADD CONSTRAINT uk_users_email UNIQUE (email);

ALTER TABLE users
  ADD CONSTRAINT uk_users_nickname UNIQUE (nickname);
