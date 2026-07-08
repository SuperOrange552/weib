-- MySQL 8 security/idempotency migration. Back up schema before execution.
DROP PROCEDURE IF EXISTS weib_security_idempotency_migrate;
DELIMITER //
CREATE PROCEDURE weib_security_idempotency_migrate()
BEGIN
  DECLARE long_usernames BIGINT DEFAULT 0;
  DECLARE idx_count BIGINT DEFAULT 0;
  SELECT COUNT(*) INTO long_usernames FROM users WHERE CHAR_LENGTH(username) > 32;
  IF long_usernames > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Migration aborted: usernames longer than 32 exist';
  END IF;

  ALTER TABLE users MODIFY COLUMN username VARCHAR(32) NOT NULL;
  ALTER TABLE users MODIFY COLUMN password VARCHAR(100) NOT NULL;
  SELECT COUNT(*) INTO idx_count FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='messages' AND column_name='client_message_id';
  IF idx_count = 0 THEN
    SET @ddl='ALTER TABLE messages ADD COLUMN client_message_id VARCHAR(64) NULL';
    PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;

  SELECT COUNT(*) INTO idx_count FROM (SELECT boss_id FROM companies GROUP BY boss_id HAVING COUNT(*) > 1) duplicates;
  IF idx_count > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Migration aborted: duplicate company boss_id values exist';
  END IF;
  SELECT COUNT(*) INTO idx_count FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='companies' AND index_name='uk_companies_boss_id';
  IF idx_count = 0 THEN
    SET @ddl='CREATE UNIQUE INDEX uk_companies_boss_id ON companies(boss_id)';
    PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;

  SELECT COUNT(*) INTO idx_count FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='messages' AND index_name='uk_message_sender_client';
  IF idx_count = 0 THEN
    SET @ddl='CREATE UNIQUE INDEX uk_message_sender_client ON messages(sender_id, client_message_id)';
    PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;
CALL weib_security_idempotency_migrate();
DROP PROCEDURE weib_security_idempotency_migrate;

-- Required domain constraints; these queries must each return at least one row before deployment completes.
SELECT index_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='applications' AND non_unique=0 AND column_name IN ('job_id','user_id');
SELECT index_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='favorite_jobs' AND non_unique=0 AND column_name IN ('job_id','user_id');
SELECT index_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='companies' AND non_unique=0 AND column_name='boss_id';