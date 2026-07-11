-- Redis cache / complaint moderation migration. Execute after backing up the database.
CREATE TABLE IF NOT EXISTS complaints (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    category VARCHAR(40) NOT NULL,
    description TEXT NOT NULL,
    evidence_urls TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT NULL,
    review_reason TEXT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_complaints_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_complaints_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_complaints_status_created
    ON complaints(status, created_at);
CREATE INDEX IF NOT EXISTS idx_complaints_target
    ON complaints(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_complaints_reporter_target
    ON complaints(reporter_id, target_type, target_id, status);

CREATE TABLE IF NOT EXISTS user_sanctions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    sanction_type VARCHAR(30) NOT NULL,
    target_type VARCHAR(20) NULL,
    target_id BIGINT NULL,
    source_complaint_id BIGINT NULL,
    reason TEXT NOT NULL,
    starts_at DATETIME NOT NULL,
    ends_at DATETIME NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    admin_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_sanctions_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_sanctions_admin FOREIGN KEY (admin_id) REFERENCES users(id),
    CONSTRAINT fk_sanctions_complaint FOREIGN KEY (source_complaint_id) REFERENCES complaints(id)
);

CREATE INDEX IF NOT EXISTS idx_sanctions_user_type_status
    ON user_sanctions(user_id, sanction_type, status);
CREATE INDEX IF NOT EXISTS idx_sanctions_window
    ON user_sanctions(starts_at, ends_at);
CREATE INDEX IF NOT EXISTS idx_sanctions_complaint
    ON user_sanctions(source_complaint_id);
