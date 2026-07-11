# 封禁申诉、管理员检索、论坛与页面布局统一设计

> 日期：2026-07-11  
> 分支：`feature/redis-cache-complaint-moderation`

## 目标

在现有 Redis 缓存、投诉审核、处罚机制和 React 管理后台基础上，增加封禁申诉、管理员统一检索、论坛社区，并建立统一的前台/后台布局规范；所有功能完成后通过自动化测试和构建验证，再合并到主分支并部署到服务器。

## 范围与验收标准

### 1. 封禁申诉

- 被禁言、禁止发布或账号封禁的用户可以提交申诉说明和证据图片/链接。
- 账号封禁用户登录后只允许访问申诉页面、申诉 API、登出，不可访问其他业务页面。
- 同一处罚同时只能存在一条 `PENDING` 申诉；申诉接口具备幂等和频控。
- 管理员可以分页查看、按状态筛选、查看证据、批准或驳回。
- 批准后撤销对应处罚；若不存在其他有效账号封禁，恢复用户可用状态；清理用户缓存并记录审计日志、发送站内通知。
- 驳回不改变原处罚，并保存审核原因。

### 2. 管理员统一检索

- 管理员可按关键词搜索用户、公司、职位、简历，支持类型筛选、分页和排序。
- 搜索结果显示头像/Logo、名称、摘要、状态、发布时间和关联 ID。
- 点击结果可以查看详情；密码、记住我 Token、验证码、CSRF 等敏感字段永不返回。
- `SUPER_ADMIN` 可以查看完整管理详情；`AUDITOR` 仅能查看脱敏后的求职者联系方式。
- 搜索结果短缓存 30 秒，写操作沿用现有延迟双删/命名空间失效策略。

### 3. 论坛社区

- 预置多个板块：求职交流、职场经验、面试经验、生活闲聊；管理员可扩展板块。
- 用户可以发布图文帖子、设置 1-5 个标签、编辑/删除自己的帖子。
- 支持帖子列表、详情、关键词/标签搜索、分页、热门排序。
- 支持评论、点赞、收藏；帖子展示发布者头像、昵称、发布时间、点赞数、评论数、收藏数。
- 图片使用受控上传接口，限制 MIME、大小和数量；文本和标签进行长度校验。
- 帖子、评论和互动使用唯一约束防重复，并通过原子计数更新保持计数一致。
- `MUTE` 禁止发帖和评论，`PUBLISH_BAN` 禁止发帖；管理员下架帖子/评论后公共接口不可见。
- 列表、详情、板块和热门计数使用 Redis 缓存；写入后立即删除并延迟二次删除。
- 论坛帖子/评论可作为投诉目标，沿用现有审核和审计链路。

### 4. 页面布局统一

- 前台建立统一的 CSS 设计令牌：字体、颜色、标题栏高度、内容最大宽度、页面内边距、卡片圆角、响应式断点。
- 所有 Thymeleaf 页面使用统一的 `app-header`、`app-main`、`app-content` 容器契约；桌面端内容宽度统一为 1200px，窄屏自动收缩。
- 管理后台使用统一 MUI theme、`PageContainer`、标题栏和分页间距，避免页面之间标题、内容宽度和留白不一致。
- 不改变现有业务字段和接口语义；布局改动必须通过页面结构检查和至少一轮桌面/窄屏视觉回归。

## API 设计

### 用户申诉

- `POST /api/appeals`
- `GET /api/appeals/mine`
- `GET /api/appeals/{id}`
- `POST /api/appeals/evidence`（multipart 图片上传）

### 管理员申诉审核

- `GET /api/admin/appeals`
- `GET /api/admin/appeals/{id}`
- `POST /api/admin/appeals/{id}/approve`
- `POST /api/admin/appeals/{id}/reject`

### 管理员统一检索

- `GET /api/admin/search?type=ALL|USER|COMPANY|JOB|RESUME&q=...&page=0&size=20`
- `GET /api/admin/search/{type}/{id}`

### 论坛

- `GET /api/forum/sections`
- `GET /api/forum/posts`
- `POST /api/forum/posts`
- `GET /api/forum/posts/{id}`
- `PUT /api/forum/posts/{id}`
- `DELETE /api/forum/posts/{id}`
- `POST /api/forum/posts/{id}/comments`
- `GET /api/forum/posts/{id}/comments`
- `POST /api/forum/posts/{id}/like` / `DELETE /api/forum/posts/{id}/like`
- `POST /api/forum/posts/{id}/favorite` / `DELETE /api/forum/posts/{id}/favorite`
- `POST /api/forum/media`

所有分页接口统一 `page`、`size`、`sort` 参数；写接口统一返回现有 `Result<T>`。

## 数据与缓存

新增表：

- `sanction_appeals`
- `forum_sections`
- `forum_posts`
- `forum_tags`
- `forum_post_tags`
- `forum_comments`
- `forum_post_likes`
- `forum_post_favorites`

申诉证据和帖子图片以 JSON 数组保存路径，原始文件存放于受控上传目录。帖子互动表使用 `(post_id,user_id)` 唯一约束；帖子计数字段由事务内原子更新维护。

缓存键使用独立命名空间：

- `cache:appeal:*`
- `cache:admin-search:*`
- `cache:forum:sections`
- `cache:forum:post:*`
- `cache:forum:posts:list:*`
- `cache:forum:post-counts:*`

缓存遵循现有 Cache-Aside、单 Key 加载锁、空值短缓存、TTL 抖动、Redis 故障回源和延迟双删策略。

## 安全、权限与可观测性

- 所有写接口校验当前用户、CSRF（Session 页面）、请求体长度、URL/文件路径、内容类型和频率。
- 管理接口沿用 JWT + `SUPER_ADMIN/AUDITOR` RBAC；永久封禁和申诉批准撤销账号封禁仅 `SUPER_ADMIN`。
- 所有申诉审核、帖子下架、处罚变更写入 `audit_logs` 并发送站内通知。
- 记录关键操作日志：用户 ID、资源 ID、结果、耗时和异常，不记录密码、Token、完整证据内容。
- 数据库迁移在生产执行前备份；迁移脚本保持可重复检查和回滚说明。

## 测试与交付

- Service/Repository 测试覆盖申诉状态机、重复申诉、处罚恢复、论坛互动幂等、计数一致性、检索脱敏。
- 集成测试覆盖缓存命中/回源、权限、敏感字段不泄露、被封禁用户只能访问申诉路径。
- 前端执行 TypeScript/Vite 构建；后端执行 `mvn test` 与 `mvn -DskipTests package`。
- 更新 Markdown/Word 接口文档和迁移说明。
- 本地验证通过后，将所有历史功能分支与本分支合并到主分支，确认主分支测试通过，再备份数据库并部署服务器，最后检查 systemd、Nginx、Redis、首页、登录、申诉、论坛和管理员检索接口。
