# 申诉、管理员检索与论坛部署说明（2026-07-11）

## 数据库迁移顺序

1. 备份生产数据库。
2. 执行 `src/main/resources/db/V20260711__cache_complaints_sanctions.sql`。
3. 执行 `src/main/resources/db/V20260712__sanction_appeals.sql`。
4. 执行 `src/main/resources/db/V20260713__forum.sql`。
5. 检查 `complaints`、`user_sanctions`、`sanction_appeals`、`forum_*` 表和索引。

## 发布前检查

- `mvn test`
- `npm run build`（工作目录 `admin-frontend`）
- `mvn -DskipTests package`
- `python scripts/verify_layout_contract.py`
- `python scripts/verify_api_practice_docs.py --allow-incomplete`
- `redis-cli ping`

## 线上验证

检查 systemd 实际运行 JAR、`journalctl -u weib.service`、Nginx upstream、Redis 连接，并依次验证首页、登录、申诉页、论坛列表/发帖/评论、管理员搜索和处罚审核。迁移失败时先停止发布，恢复数据库备份和上一版本 JAR。