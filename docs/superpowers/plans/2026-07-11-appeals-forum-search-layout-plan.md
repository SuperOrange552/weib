# 申诉、管理员检索、论坛与页面布局统一实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `$superpower-executing-plans` to implement this plan task-by-task. Each task must be tracked with `update_plan`; follow TDD (write a failing test before production code).

**Goal:** 在现有处罚/投诉/Redis/React 管理后台基础上交付封禁申诉、管理员统一检索、论坛社区和全站布局规范，并完成主分支合并与服务器部署验证。

**Architecture:** 继续使用 Spring Boot + JPA + Session/JWT RBAC。申诉、论坛和检索分别建立独立实体/服务/控制器；公共读链路使用现有 Cache-Aside 与延迟双删；前台复用 Thymeleaf，管理端复用 React + MUI。所有写操作经过 DTO 校验、频控、权限和审计。

**Tech Stack:** Java 17/21, Spring Boot 3.2, Spring Data JPA, MySQL, Redis, Thymeleaf, React, TypeScript, MUI, Maven, Vite, JUnit/Mockito。

---

### Task 0: 基线与工作区保护

**Files:**
- Read: `README.md`, `pom.xml`, `src/main/resources/application.yml`
- Test: existing `src/test/java/**`

- [ ] 记录当前分支、主分支未提交改动和 worktree 路径，禁止覆盖主分支脏文件。
- [ ] 运行 `mvn test` 和 `npm run build`，保存基线结果。
- [ ] 后续每个任务完成后运行相关测试并提交一个小 commit。

### Task 1: 封禁申诉数据层（TDD）

**Files:**
- Create: `src/main/resources/db/V20260711__appeals_forum_search.sql`
- Create: `src/main/java/com/weib/entity/SanctionAppeal.java`
- Create: `src/main/java/com/weib/repository/SanctionAppealRepository.java`
- Create: `src/main/java/com/weib/dto/AppealCreateRequest.java`, `AppealResponse.java`
- Test: `src/test/java/com/weib/repository/SanctionAppealRepositoryTest.java`

- [ ] 先写重复 pending 申诉、状态查询和证据 JSON 读写测试并确认失败。
- [ ] 创建 `sanction_appeals` 表、索引和 `(sanction_id,status)` 查询约束。
- [ ] 实现实体、仓储和 DTO 校验：理由 2-2000 字，证据最多 5 个受控 URL。
- [ ] 运行仓储测试和 `git diff --check`，提交 `feat: add sanction appeal persistence`。

### Task 2: 封禁申诉业务、权限和接口（TDD）

**Files:**
- Create: `src/main/java/com/weib/service/AppealService.java`
- Create: `src/main/java/com/weib/controller/AppealController.java`
- Create: `src/main/java/com/weib/controller/admin/AdminAppealController.java`
- Create: `src/main/java/com/weib/dto/admin/AppealReviewRequest.java`, `AppealListResponse.java`
- Modify: `LoginInterceptor.java`, `AdminSecurityConfig.java`, `GlobalExceptionHandler.java`
- Test: `src/test/java/com/weib/service/AppealServiceTest.java`, `src/test/java/com/weib/integration/AppealAccessIntegrationTest.java`

- [ ] 先写状态机测试：创建 pending、重复拒绝、批准撤销处罚、保留其他处罚、通知/审计和频控。
- [ ] 调整登录拦截：账号封禁用户可建立受限 session，只能访问申诉页/API/登出；其余路径继续阻断。
- [ ] 实现用户接口 `/api/appeals`、证据上传和管理员审核接口；永久账号封禁的批准/撤销仅 SUPER_ADMIN。
- [ ] 批准时更新 sanction、用户状态、缓存、审计日志和站内通知，事务失败回滚。
- [ ] 运行单测/集成测试并提交 `feat: add sanction appeal workflow`。

### Task 3: 管理员统一检索后端（TDD）

**Files:**
- Create: `src/main/java/com/weib/dto/admin/AdminSearchResponse.java`, `AdminSearchDetailResponse.java`
- Create: `src/main/java/com/weib/service/admin/AdminSearchService.java`
- Create: `src/main/java/com/weib/controller/admin/AdminSearchController.java`
- Modify: `UserRepository.java`, `CompanyRepository.java`, `JobRepository.java`, `ResumeRepository.java`, `AdminSecurityConfig.java`
- Test: `src/test/java/com/weib/service/admin/AdminSearchServiceTest.java`, `src/test/java/com/weib/integration/AdminSearchSecurityTest.java`

- [ ] 先写关键词/类型/分页/脱敏测试，确认 password、rememberToken、captcha、csrf 不出现在 JSON。
- [ ] 实现 `GET /api/admin/search` 和 `GET /api/admin/search/{type}/{id}`；支持 USER/COMPANY/JOB/RESUME/ALL。
- [ ] SUPER_ADMIN 返回完整管理详情，AUDITOR 对联系方式脱敏；所有查询参数限制长度和 page/size 上限。
- [ ] 将搜索结果接入短 TTL Redis 缓存，写操作沿用现有命名空间失效。
- [ ] 运行测试并提交 `feat: add admin unified search`。

### Task 4: 管理员检索前端（TDD）

**Files:**
- Create: `admin-frontend/src/api/search.ts`
- Create: `admin-frontend/src/types/search.ts`
- Create: `admin-frontend/src/pages/GlobalSearchPage.tsx`
- Modify: `admin-frontend/src/App.tsx`, `components/AdminLayout.tsx`, `types/index.ts`
- Test: `admin-frontend` TypeScript/Vite build

- [ ] 先定义 API 类型和空/错误/分页状态测试样例。
- [ ] 实现统一搜索框、类型 Tabs、结果表格、详情 Drawer、联系方式脱敏提示。
- [ ] 加入路由 `/admin/search` 和侧边栏入口，复用现有 MUI 表格/分页规范。
- [ ] 运行 `npm run build` 并提交 `feat: add admin search page`。

### Task 5: 论坛数据层与缓存键（TDD）

**Files:**
- Modify: `src/main/resources/db/V20260711__appeals_forum_search.sql`
- Create: `ForumSection.java`, `ForumPost.java`, `ForumTag.java`, `ForumComment.java`, `ForumPostLike.java`, `ForumPostFavorite.java`
- Create: corresponding repositories under `src/main/java/com/weib/repository/`
- Modify: `CacheKeys.java`
- Test: repository tests for unique interactions and counters

- [ ] 先写帖子、标签、评论、点赞/收藏唯一约束和计数边界测试，确认失败。
- [ ] 创建板块、帖子、标签、关联、评论、点赞、收藏表及索引，插入四个默认板块。
- [ ] 实现实体/仓储和原子计数更新查询；帖子状态支持 ACTIVE/HIDDEN/DELETED。
- [ ] 添加论坛缓存键和列表/详情命名空间失效方法。
- [ ] 运行仓储测试并提交 `feat: add forum persistence`。

### Task 6: 论坛业务、上传、权限与接口（TDD）

**Files:**
- Create: `ForumService.java`, `ForumController.java`, `ForumMediaController.java`
- Create: forum DTOs under `src/main/java/com/weib/dto/forum/`
- Modify: `SanctionService.java`, `AdminComplaintService.java`, `ComplaintCreateRequest.java`
- Test: `ForumServiceTest.java`, `ForumInteractionIntegrationTest.java`, upload validation tests

- [ ] 先写发帖/评论/点赞/收藏幂等、MUTE/PUBLISH_BAN、隐藏帖子不可见、搜索排序和计数一致性测试。
- [ ] 实现板块、帖子、评论、互动和搜索接口；所有公共返回使用 `PublicUserProfile` 安全字段。
- [ ] 实现图片 multipart 上传：仅允许图片 MIME、单文件上限、总数量上限、路径归一化和目录隔离。
- [ ] 事务内更新计数，Redis 使用 Cache-Aside、加载锁、TTL 抖动和延迟双删；Redis 故障回源数据库。
- [ ] 扩展投诉目标类型支持论坛帖子/评论，并让管理员下架后清理相关缓存。
- [ ] 运行测试并提交 `feat: add forum community APIs`。

### Task 7: 论坛前台页面（TDD）

**Files:**
- Create: `src/main/resources/templates/forum.html`, `forum-detail.html`, `forum-compose.html`
- Create: `src/main/resources/static/js/forum.js`, `forum.css`
- Modify: `IndexController.java`, common navigation templates
- Test: HTML/JS contract checks and `mvn test`

- [ ] 先写页面契约检查：板块切换、搜索、帖子卡片头像/时间/点赞/评论/收藏、评论发布。
- [ ] 实现板块导航、帖子列表/搜索、编辑器、图片预览、详情和评论互动。
- [ ] 对未登录访问只读，对登录用户展示发布/互动；处罚用户显示明确原因和申诉入口。
- [ ] 运行页面契约检查与后端测试，提交 `feat: add forum pages`。

### Task 8: 前台与后台布局统一（TDD/视觉回归）

**Files:**
- Create: `src/main/resources/static/css/app-shell.css`
- Create: `src/main/resources/templates/fragments/layout.html`（如现有模板可安全复用）
- Modify: all `src/main/resources/templates/*.html` pages
- Create: `admin-frontend/src/theme.ts`, `components/PageContainer.tsx`
- Modify: `admin-frontend/src/components/AdminLayout.tsx`, all admin pages
- Test: `scripts/verify_layout_contract.py`, browser/截图回归（可用时）

- [ ] 先写布局契约脚本，检查每个页面有统一 header/main/content 类、唯一 h1 和公共 CSS 引用。
- [ ] 建立 CSS 变量、1200px 桌面容器、窄屏断点、统一标题栏/卡片/分页间距。
- [ ] 批量迁移 Thymeleaf 页面并保留业务专属样式；消除页面级 max-width/标题栏冲突。
- [ ] 建立 MUI theme 和 PageContainer，统一管理端页面标题、内容宽度和间距。
- [ ] 运行契约检查、`npm run build`，条件允许时做桌面/移动截图回归，提交 `refactor: normalize page layout`。

### Task 9: 文档、全量测试与发布候选包

**Files:**
- Modify: `docs/API_TESTING_COMPLETE.md`, `docs/微招系统完整接口测试文档.docx`
- Create: `docs/deployment/2026-07-11-appeals-forum-search.md`
- Test: all Java/TypeScript tests and API-doc verifier

- [ ] 为申诉、检索、论坛接口补齐 Apifox/Postman 请求体、状态码、权限和边界值。
- [ ] 更新数据库迁移顺序、Redis 键、日志/回滚和部署检查清单。
- [ ] 运行 `mvn test`、`npm run build`、`mvn -DskipTests package`、`python scripts/verify_api_practice_docs.py --allow-incomplete`。
- [ ] 记录 JAR SHA256、工作区状态和测试结果，提交 `docs: document appeals forum search and layout`。

### Task 10: 合并主分支与服务器部署

**Files:**
- Main repo: `C:\Users\cg\Desktop\weib`
- Server credentials: `C:\Users\cg\Desktop\新建文件夹\openclaw.md`（仅本地读取，不写入仓库）

- [ ] 先为主分支当前未提交的安全/验证码改动创建补丁或临时保存，确认可恢复。
- [ ] 将本分支与历史安全、验证码、API 文档分支按依赖顺序整合到主分支，解决重叠文件并在合并后重新测试。
- [ ] 读取服务器账号后先备份数据库和当前 JAR，执行迁移脚本，上传发布候选 JAR/静态资源，重启 systemd 服务。
- [ ] 检查 `systemctl`, `journalctl`, `redis-cli ping`, Nginx upstream、首页、登录、申诉、论坛、管理员检索。
- [ ] 只有线上检查全部通过后，报告主分支提交、服务器版本、迁移结果和回滚路径。
