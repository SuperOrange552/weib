-- Dual business identities. Safe to run repeatedly after a database backup.
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    enabled_at DATETIME NULL,
    enabled_by BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_type),
    KEY idx_user_roles_status (role_type, status),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS role_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_type VARCHAR(20) NOT NULL,
    nickname VARCHAR(50) NULL,
    avatar VARCHAR(500) NULL,
    bio VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_role_profile (user_id, role_type),
    CONSTRAINT fk_role_profiles_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- seeker_ahua and legacy boss_* accounts receive BOSS.
INSERT INTO user_roles(user_id, role_type, status, enabled_at, created_at, updated_at)
SELECT u.id, 'BOSS', 'ACTIVE', NOW(), NOW(), NOW()
FROM users u
WHERE (u.username = 'seeker_ahua' OR u.username LIKE 'boss\_%')
  AND LOWER(COALESCE(u.role, '')) <> 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles r WHERE r.user_id = u.id AND r.role_type = 'BOSS'
  );

-- seeker_ahua and all legacy seeker accounts receive SEEKER; boss_* stay BOSS-only.
INSERT INTO user_roles(user_id, role_type, status, enabled_at, created_at, updated_at)
SELECT u.id, 'SEEKER', 'ACTIVE', NOW(), NOW(), NOW()
FROM users u
WHERE (u.username = 'seeker_ahua' OR LOWER(COALESCE(u.role, '')) = 'seeker')
  AND u.username NOT LIKE 'boss\_%'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles r WHERE r.user_id = u.id AND r.role_type = 'SEEKER'
  );

INSERT INTO role_profiles(user_id, role_type, nickname, avatar, created_at, updated_at)
SELECT r.user_id, r.role_type, COALESCE(u.nickname, u.username), u.avatar, NOW(), NOW()
FROM user_roles r
JOIN users u ON u.id = r.user_id
WHERE NOT EXISTS (
    SELECT 1 FROM role_profiles p WHERE p.user_id = r.user_id AND p.role_type = r.role_type
);
