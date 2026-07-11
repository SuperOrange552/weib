CREATE TABLE IF NOT EXISTS notification_events (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, event_id VARCHAR(64) NOT NULL, recipient_id BIGINT NOT NULL,
 recipient_role VARCHAR(20) NOT NULL, event_type VARCHAR(50) NOT NULL, related_id BIGINT NULL,
 title VARCHAR(500) NOT NULL, payload TEXT NULL, is_read TINYINT(1) NOT NULL DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_notification_event_id(event_id), KEY idx_notification_recipient(recipient_id,recipient_role,id));
CREATE TABLE IF NOT EXISTS mobile_push_tokens (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT NOT NULL,active_role VARCHAR(20) NOT NULL,
 installation_id VARCHAR(100) NOT NULL,push_token VARCHAR(500) NOT NULL,status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uk_push_installation(installation_id),KEY idx_push_identity(user_id,active_role,status));
