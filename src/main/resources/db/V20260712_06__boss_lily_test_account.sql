-- Idempotent production-practice account requested for Web testing.
-- Login: boss_lily / Weib@123456 (the value stored below is BCrypt, never plaintext).
INSERT INTO users (
    username, password, role, nickname, status, login_fail_count, created_at, updated_at
)
SELECT
    'boss_lily',
    '$2b$10$rb5oFX5K6qYlfde/v5vFFOkIodOggkABHo43mow.dAAJfUQwwdN1m',
    'boss', '莉莉Boss', 'active', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'boss_lily');

INSERT INTO user_roles (user_id, role_type, status, enabled_at, created_at, updated_at)
SELECT u.id, 'BOSS', 'ACTIVE', NOW(), NOW(), NOW()
FROM users u
WHERE u.username = 'boss_lily'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles r WHERE r.user_id = u.id AND r.role_type = 'BOSS'
  );

INSERT INTO role_profiles (user_id, role_type, nickname, avatar, bio, created_at, updated_at)
SELECT u.id, 'BOSS', '莉莉Boss', u.avatar, '招聘者测试账号', NOW(), NOW()
FROM users u
WHERE u.username = 'boss_lily'
  AND NOT EXISTS (
      SELECT 1 FROM role_profiles p WHERE p.user_id = u.id AND p.role_type = 'BOSS'
  );
