-- ============================================
-- 微招（weib）管理后台 — 数据库迁移脚本
-- ============================================
-- 说明：此脚本为管理后台功能的数据库变更，
--       使用 IF NOT EXISTS 保证幂等性（可重复执行）
-- ============================================

-- ============================================
-- 1. User 表变更
-- ============================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'active';
CREATE INDEX IF NOT EXISTS idx_users_role_status ON users(role, status);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
UPDATE users SET status = 'active' WHERE status IS NULL;

-- ============================================
-- 2. Company 表变更（审核字段）
-- ============================================
ALTER TABLE companies ADD COLUMN IF NOT EXISTS audit_status VARCHAR(20) NOT NULL DEFAULT 'pending';
ALTER TABLE companies ADD COLUMN IF NOT EXISTS audit_reason VARCHAR(500);
CREATE INDEX IF NOT EXISTS idx_companies_audit_status ON companies(audit_status);
UPDATE companies SET audit_status = 'approved' WHERE audit_status = 'pending' AND id IS NOT NULL;

-- ============================================
-- 3. Job 表变更（审核字段）
-- ============================================
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS audit_status VARCHAR(20) NOT NULL DEFAULT 'pending';
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS audit_reason VARCHAR(500);
CREATE INDEX IF NOT EXISTS idx_jobs_audit_status ON jobs(audit_status);
UPDATE jobs SET audit_status = 'approved' WHERE audit_status = 'pending' AND id IS NOT NULL;

-- ============================================
-- 4. AdminRole 表（新建）
-- ============================================
CREATE TABLE IF NOT EXISTS admin_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    role_type VARCHAR(20) NOT NULL DEFAULT 'viewer',
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_admin_roles_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_admin_roles_user_id ON admin_roles(user_id);

-- ============================================
-- 5. AuditLog 表（新建）
-- ============================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT,
    reason TEXT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_audit_logs_admin FOREIGN KEY (admin_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_admin_id ON audit_logs(admin_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_target ON audit_logs(target_type, target_id);

-- ============================================
-- 6. 初始化超级管理员
-- ============================================
-- 密码: Admin@123456（BCrypt 加密）
INSERT INTO users (username, password, role, nickname, status, created_at, updated_at)
SELECT 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin', '超级管理员', 'active', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO admin_roles (user_id, role_type, created_at)
SELECT id, 'super_admin', NOW() FROM users WHERE username = 'admin'
AND NOT EXISTS (SELECT 1 FROM admin_roles ar JOIN users u ON ar.user_id = u.id WHERE u.username = 'admin');
