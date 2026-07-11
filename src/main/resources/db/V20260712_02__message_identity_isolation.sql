ALTER TABLE messages ADD COLUMN sender_role VARCHAR(20) NULL AFTER sender_id;
ALTER TABLE messages ADD COLUMN receiver_role VARCHAR(20) NULL AFTER receiver_id;

UPDATE messages m
JOIN applications a ON m.conversation_id = CONCAT('app_', a.id)
JOIN jobs j ON a.job_id = j.id
JOIN companies c ON j.company_id = c.id
SET m.sender_role = CASE WHEN m.sender_id = a.user_id THEN 'SEEKER' ELSE 'BOSS' END,
    m.receiver_role = CASE WHEN m.receiver_id = a.user_id THEN 'SEEKER' ELSE 'BOSS' END
WHERE m.sender_role IS NULL OR m.receiver_role IS NULL;

UPDATE messages SET sender_role = 'SEEKER' WHERE sender_role IS NULL;
UPDATE messages SET receiver_role = 'BOSS' WHERE receiver_role IS NULL;
ALTER TABLE messages MODIFY sender_role VARCHAR(20) NOT NULL;
ALTER TABLE messages MODIFY receiver_role VARCHAR(20) NOT NULL;
ALTER TABLE messages DROP INDEX uk_message_sender_client;
ALTER TABLE messages ADD UNIQUE KEY uk_message_sender_client (sender_id, sender_role, client_message_id);
