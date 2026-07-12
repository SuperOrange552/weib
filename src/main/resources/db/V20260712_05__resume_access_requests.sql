CREATE TABLE IF NOT EXISTS resume_access_requests (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 seeker_id BIGINT NOT NULL,
 boss_id BIGINT NOT NULL,
 company_id BIGINT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 decided_at DATETIME NULL,
 UNIQUE KEY uk_resume_access_pair(seeker_id,boss_id),
 KEY idx_resume_access_seeker(seeker_id,status),
 KEY idx_resume_access_boss(boss_id,status)
);
