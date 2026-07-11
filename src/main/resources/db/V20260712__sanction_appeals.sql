-- Sanction appeal workflow. Back up production before execution.
CREATE TABLE IF NOT EXISTS sanction_appeals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sanction_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    evidence_urls TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT NULL,
    review_reason TEXT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_appeals_sanction FOREIGN KEY (sanction_id) REFERENCES user_sanctions(id),
    CONSTRAINT fk_appeals_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_appeals_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_appeals_user_created ON sanction_appeals(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_appeals_status_created ON sanction_appeals(status, created_at);
CREATE INDEX IF NOT EXISTS idx_appeals_sanction_status ON sanction_appeals(sanction_id, status);