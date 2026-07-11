ALTER TABLE forum_posts ADD COLUMN author_role VARCHAR(20) NULL AFTER author_id;
ALTER TABLE forum_comments ADD COLUMN author_role VARCHAR(20) NULL AFTER author_id;
UPDATE forum_posts p JOIN users u ON p.author_id=u.id SET p.author_role=CASE WHEN LOWER(u.role)='boss' THEN 'BOSS' ELSE 'SEEKER' END WHERE p.author_role IS NULL;
UPDATE forum_comments c JOIN users u ON c.author_id=u.id SET c.author_role=CASE WHEN LOWER(u.role)='boss' THEN 'BOSS' ELSE 'SEEKER' END WHERE c.author_role IS NULL;
ALTER TABLE forum_posts MODIFY author_role VARCHAR(20) NOT NULL;
ALTER TABLE forum_comments MODIFY author_role VARCHAR(20) NOT NULL;
CREATE INDEX idx_forum_posts_author_identity ON forum_posts(author_id,author_role,created_at);
CREATE INDEX idx_forum_comments_author_identity ON forum_comments(author_id,author_role,created_at);
