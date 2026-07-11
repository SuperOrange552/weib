# 微招双身份、单端会话与 App 完整化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use $superpower-subagents (recommended) or $superpower-executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking via update_plan.

**Goal:** 在不破坏现有 Web 与生产数据的前提下，实现账号多身份、Web/Mobile 单端会话互踢、身份隔离通知、在线简历授权，并补齐 Android 求职者和 Boss 的全部既定功能。

**Architecture:** `users` 只负责认证，`user_roles` 和 `role_profiles` 负责身份授权与展示；JWT/Session 保存本次登录的 `activeRole`，Redis 按 `userId + clientType` 保存唯一会话槽位。业务事件先持久化到 `notification_events`，再由 WebSocket、FCM 和 WorkManager 分层送达；在线简历通过公开摘要、请求、授权和审计表控制完整资料访问。

**Tech Stack:** Java 17、Spring Boot 3.2.5、Spring MVC、JPA、MySQL 8、Redis、JWT、WebSocket/STOMP、JUnit 5、MockMvc；React 18、TypeScript、MUI；Kotlin 2.0、Jetpack Compose、Retrofit、OkHttp、DataStore、WorkManager、Firebase Messaging（配置可降级）、Gradle 8.10.2。

**Spec:** `docs/superpowers/specs/2026-07-12-dual-role-session-notification-resume-design.md`

---

## 文件结构

- `src/main/java/com/weib/identity/**`：角色、身份资料与当前身份上下文。
- `src/main/java/com/weib/session/**`：终端类型、Redis 会话槽位、互踢原因和校验拦截器。
- `src/main/java/com/weib/notification/**`：通知事件持久化、实时发布、推送令牌和未读查询。
- `src/main/java/com/weib/resumeaccess/**`：在线简历搜索、请求、授权和审计。
- `src/main/java/com/weib/controller/mobile/**`：移动端 JSON 契约；不得复用 HTML 返回值。
- `android-app/app/src/main/java/com/weib/app/feature/**`：按 auth、seeker、boss、chat、forum、moderation、resume 分包。
- `admin-frontend/src/pages/**` 与 `admin-frontend/src/api/**`：管理员身份、会话、通知和简历审计页面。

---

### Task 1: 创建多身份数据模型和幂等迁移

**Files:**
- Create: `src/main/resources/db/V20260712__dual_role_identity.sql`
- Create: `src/main/java/com/weib/entity/UserRole.java`
- Create: `src/main/java/com/weib/entity/RoleProfile.java`
- Create: `src/main/java/com/weib/repository/UserRoleRepository.java`
- Create: `src/main/java/com/weib/repository/RoleProfileRepository.java`
- Create: `src/main/java/com/weib/service/IdentityService.java`
- Test: `src/test/java/com/weib/service/IdentityServiceTest.java`
- Test: `src/test/java/com/weib/integration/DualRoleMigrationIntegrationTest.java`

- [ ] **Step 1: 写身份服务失败测试**

```java
@Test void seekerAhuaHasBothRolesAndBossZhangOnlyBoss() {
    assertEquals(Set.of("SEEKER", "BOSS"), service.enabledRoles(ahuaId));
    assertEquals(Set.of("BOSS"), service.enabledRoles(bossZhangId));
}

@Test void roleMustBeEnabled() {
    assertThrows(RoleNotEnabledException.class,
        () -> service.requireEnabledRole(normalSeekerId, "BOSS"));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -Dtest=IdentityServiceTest,DualRoleMigrationIntegrationTest test`  
Expected: FAIL，缺少 `IdentityService`、实体和新表。

- [ ] **Step 3: 创建标准化实体和唯一约束**

`user_roles` 使用 `(user_id, role_type)` 唯一约束；`role_profiles` 使用相同组合唯一约束。`IdentityService.requireEnabledRole(userId, role)` 只接受 `SEEKER/BOSS` 且状态必须为 `ACTIVE`。

- [ ] **Step 4: 编写可重复执行的数据迁移**

迁移逻辑必须使用 `INSERT ... SELECT ... WHERE NOT EXISTS`：

```sql
INSERT INTO user_roles(user_id, role_type, status, enabled_at, created_at, updated_at)
SELECT id, 'BOSS', 'ACTIVE', NOW(), NOW(), NOW()
FROM users u
WHERE (u.username = 'seeker_ahua' OR u.username LIKE 'boss\_%')
AND NOT EXISTS (
  SELECT 1 FROM user_roles r WHERE r.user_id = u.id AND r.role_type = 'BOSS'
);
```

另为 `seeker_ahua` 和非 `boss_*` 普通用户写入 `SEEKER`；管理员账号不写入业务角色。

- [ ] **Step 5: 运行迁移和全量后端测试**

Run: `mvn test`  
Expected: 新旧 47+ 测试全部通过，重复执行迁移不产生重复记录。

- [ ] **Step 6: 提交**

```bash
git add src/main/resources/db src/main/java/com/weib/entity src/main/java/com/weib/repository src/main/java/com/weib/service src/test
git commit -m "feat: add normalized seeker and boss identities"
```

---

### Task 2: 改造 Web 与移动端角色选择登录

**Files:**
- Modify: `src/main/java/com/weib/controller/UserController.java`
- Modify: `src/main/java/com/weib/controller/mobile/MobileAuthController.java`
- Modify: `src/main/java/com/weib/dto/mobile/MobileLoginRequest.java`
- Modify: `src/main/java/com/weib/dto/mobile/MobileLoginResponse.java`
- Modify: `src/main/java/com/weib/util/JwtUtil.java`
- Modify: `src/main/resources/templates/login.html`
- Modify: `android-app/app/src/main/java/com/weib/app/data/Models.kt`
- Modify: `android-app/app/src/main/java/com/weib/app/ui/WeibApp.kt`
- Test: `src/test/java/com/weib/controller/mobile/MobileAuthControllerTest.java`
- Test: `src/test/java/com/weib/controller/UserControllerRoleLoginTest.java`
- Test: `android-app/app/src/test/java/com/weib/app/LoginRoleSelectionTest.kt`

- [ ] **Step 1: 写登录契约失败测试**

```java
mockMvc.perform(post("/api/mobile/auth/login")
  .contentType(APPLICATION_JSON)
  .content("{\"username\":\"seeker_ahua\",\"password\":\"Pass1234\",\"captcha\":\"AB12\",\"selectedRole\":\"BOSS\"}"))
  .andExpect(jsonPath("$.data.user.activeRole").value("BOSS"));

mockMvc.perform(post("/api/mobile/auth/login")
  .contentType(APPLICATION_JSON)
  .content("{\"username\":\"seeker_lily\",\"password\":\"Pass1234\",\"captcha\":\"AB12\",\"selectedRole\":\"BOSS\"}"))
  .andExpect(jsonPath("$.code").value(403))
  .andExpect(jsonPath("$.data.reason").value("ROLE_NOT_ENABLED"));
```

- [ ] **Step 2: 运行 Web、移动端和 Android 测试确认失败**

Run: `mvn -Dtest=MobileAuthControllerTest,UserControllerRoleLoginTest test`  
Run: `android-app/gradlew testProdDebugUnitTest --tests "*LoginRoleSelectionTest"`

- [ ] **Step 3: 扩展 JWT 与 Session 身份上下文**

JWT 添加 `activeRole`、`clientType` 和 `sid`。登录成功后 Session 设置：

```java
newSession.setAttribute("user", user);
newSession.setAttribute("activeRole", selectedRole);
newSession.setAttribute("clientType", "WEB");
newSession.setAttribute("sid", sessionTokenId);
```

- [ ] **Step 4: 修改登录 UI**

Web 与 Compose 登录页使用求职者/Boss 单选卡；默认 SEEKER。提交按钮前端只检查选择存在，是否开通完全由后端决定。角色切换不得出现在已登录页面。

- [ ] **Step 5: 运行测试并提交**

Run: `mvn test`  
Run: `android-app/gradlew testProdDebugUnitTest assembleProdDebug`

```bash
git commit -am "feat: add role selection to web and mobile login"
```

---

### Task 3: 实现 Redis Web/Mobile 唯一会话和实时互踢

**Files:**
- Create: `src/main/java/com/weib/session/ClientType.java`
- Create: `src/main/java/com/weib/session/SessionInvalidationReason.java`
- Create: `src/main/java/com/weib/session/LoginSlot.java`
- Create: `src/main/java/com/weib/session/SessionRegistryService.java`
- Create: `src/main/java/com/weib/session/SessionSlotInterceptor.java`
- Modify: `src/main/java/com/weib/config/WebConfig.java`
- Modify: `src/main/java/com/weib/config/LoginInterceptor.java`
- Modify: `src/main/java/com/weib/config/WebSocketConfig.java`
- Modify: `src/main/java/com/weib/service/UserService.java`
- Test: `src/test/java/com/weib/session/SessionRegistryServiceTest.java`
- Test: `src/test/java/com/weib/session/SessionSlotInterceptorTest.java`

- [ ] **Step 1: 写并发槽位失败测试**

```java
@Test void mobileLoginReplacesOnlyPreviousMobileSlot() {
    registry.register(userId, MOBILE, "m1", "SEEKER", device1);
    SessionReplacement replacement = registry.register(userId, MOBILE, "m2", "BOSS", device2);
    assertEquals("m1", replacement.replacedSid());
    assertEquals("m2", registry.current(userId, MOBILE).sid());
    assertNull(registry.current(userId, WEB));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -Dtest=SessionRegistryServiceTest,SessionSlotInterceptorTest test`

- [ ] **Step 3: 使用 Lua 原子替换 Redis 槽位**

键为 `login:slot:{userId}:{WEB|MOBILE}`。Lua 返回旧值后写新值并设置 TTL，避免并发登录造成两个有效 Token。

- [ ] **Step 4: 每个受保护请求校验 sid**

不匹配时返回：

```json
{"code":401,"msg":"你的账号已在其他设备登录","data":{"reason":"KICKED"}}
```

Web 跳转 `/login?kicked`；App 全局清理 DataStore Token 并弹出安全提醒。

- [ ] **Step 5: 发布强制下线事件**

向 `/user/{userId}/queue/security` 发布包含旧 `sid`、`clientType`、登录时间和提示文案的 `FORCE_LOGOUT`，旧端仅在 sid 匹配时退出。

- [ ] **Step 6: 密码修改、封禁和管理员强退清除两个槽位**

`SessionRegistryService.invalidateAll(userId, PASSWORD_CHANGED|ACCOUNT_BANNED|ADMIN_FORCED)` 必须删除 WEB/MOBILE 并发布事件。

- [ ] **Step 7: 测试并提交**

Run: `mvn test`

```bash
git commit -am "feat: enforce one web and one mobile session per account"
```

---

### Task 4: 将所有业务授权切换到 activeRole

**Files:**
- Create: `src/main/java/com/weib/identity/ActiveIdentity.java`
- Create: `src/main/java/com/weib/identity/ActiveIdentityResolver.java`
- Modify: `src/main/java/com/weib/controller/SeekerApiController.java`
- Modify: `src/main/java/com/weib/controller/BossController.java`
- Modify: `src/main/java/com/weib/controller/mobile/MobileAccessPolicy.java`
- Modify: `src/main/java/com/weib/controller/mobile/MobileBossController.java`
- Modify: `src/main/java/com/weib/controller/mobile/MobileSeekerActionController.java`
- Modify: `src/main/java/com/weib/controller/ChatController.java`
- Modify: `src/main/java/com/weib/controller/ForumController.java`
- Modify: `src/main/java/com/weib/controller/ComplaintController.java`
- Modify: `src/main/java/com/weib/controller/AppealController.java`
- Test: `src/test/java/com/weib/security/ActiveRoleAuthorizationTest.java`

- [ ] **Step 1: 写身份隔离失败测试**

同一 `userId` 分别创建 SEEKER/BOSS Session，验证求职者不能访问 Boss 公司数据、Boss 不能读取求职者收藏和投递，论坛作者资料取当前身份资料。

- [ ] **Step 2: 运行测试确认旧 `user.role` 导致失败**

Run: `mvn -Dtest=ActiveRoleAuthorizationTest test`

- [ ] **Step 3: 实现统一解析器**

```java
public record ActiveIdentity(Long userId, String role, String nickname, String avatar) {}

public ActiveIdentity require(HttpSession session, String expectedRole) {
    User user = (User) session.getAttribute("user");
    String activeRole = (String) session.getAttribute("activeRole");
    identityService.requireEnabledRole(user.getId(), activeRole);
    if (!expectedRole.equals(activeRole)) throw new AccessDeniedException("ROLE_MISMATCH");
    return profileService.resolve(user.getId(), activeRole);
}
```

- [ ] **Step 4: 替换所有 `user.getRole()` 授权判断**

仅数据迁移兼容层可以读取旧 role；Controller、Service 和缓存键不得用旧 role 授权。

- [ ] **Step 5: 将聊天会话参与者升级为身份级参与者**

消息保存 `sender_role`、`receiver_role`；会话唯一性包含双方 `userId + role`，防止同账号双身份消息互通。

- [ ] **Step 6: 测试并提交**

Run: `mvn test`

```bash
git commit -am "refactor: authorize all business actions by active identity"
```

---

### Task 5: 升级管理后台身份、会话和审计权限

**Files:**
- Create: `src/main/java/com/weib/controller/admin/AdminIdentityController.java`
- Create: `src/main/java/com/weib/controller/admin/AdminSessionController.java`
- Create: `src/main/java/com/weib/controller/admin/AdminNotificationController.java`
- Create: `src/main/java/com/weib/service/admin/AdminIdentityService.java`
- Modify: `src/main/java/com/weib/config/AdminSecurityConfig.java`
- Modify: `src/main/java/com/weib/service/admin/AdminSearchService.java`
- Modify: `admin-frontend/src/api/users.ts`
- Create: `admin-frontend/src/api/sessions.ts`
- Create: `admin-frontend/src/api/notifications.ts`
- Modify: `admin-frontend/src/pages/UserDetailDrawer.tsx`
- Test: `src/test/java/com/weib/service/admin/AdminIdentityServiceTest.java`
- Test: `src/test/java/com/weib/controller/admin/AdminIdentityAuthorizationTest.java`

- [ ] **Step 1: 写 RBAC 失败测试**

验证 `super_admin` 可开通/禁用身份和强退会话；`auditor` 只能基于企业审核激活 Boss；`viewer` 所有写接口返回 403。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -Dtest=AdminIdentityServiceTest,AdminIdentityAuthorizationTest test`

- [ ] **Step 3: 实现管理员接口和审计**

身份变更、强制下线、通知重试和授权撤销均要求 `reason`、`Idempotency-Key`，并写入 `audit_logs` 的 before/after JSON。

- [ ] **Step 4: 保证企业审核与 Boss 激活原子性**

`@Transactional` 服务在公司审核为 approved 后激活 BOSS；任一步失败整体回滚。

- [ ] **Step 5: 更新管理后台页面**

用户详情抽屉增加“身份与权限、在线会话、通知记录、简历授权审计”标签；根据管理员角色禁用或隐藏操作，同时后端继续强制鉴权。

- [ ] **Step 6: 构建并提交**

Run: `mvn test`  
Run: `cd admin-frontend && npm run build`

```bash
git commit -am "feat: manage identities sessions and audits in admin"
```

---

### Task 6: 建立可靠通知事件中心

**Files:**
- Create: `src/main/resources/db/V20260712__notification_events.sql`
- Create: `src/main/java/com/weib/entity/NotificationEvent.java`
- Create: `src/main/java/com/weib/entity/MobilePushToken.java`
- Create: `src/main/java/com/weib/repository/NotificationEventRepository.java`
- Create: `src/main/java/com/weib/repository/MobilePushTokenRepository.java`
- Create: `src/main/java/com/weib/notification/NotificationEventService.java`
- Create: `src/main/java/com/weib/notification/RealtimeNotificationPublisher.java`
- Create: `src/main/java/com/weib/notification/PushGateway.java`
- Create: `src/main/java/com/weib/controller/mobile/MobileNotificationController.java`
- Test: `src/test/java/com/weib/notification/NotificationEventServiceTest.java`

- [ ] **Step 1: 写事件持久化和隔离失败测试**

同一账号 SEEKER/BOSS 各写一条事件，查询 SEEKER 未读只能返回 SEEKER 事件；重复 `eventId` 只能保存一次。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -Dtest=NotificationEventServiceTest test`

- [ ] **Step 3: 实现事务后发布**

业务事务保存事件；使用 `@TransactionalEventListener(phase = AFTER_COMMIT)` 调用 WebSocket 和 PushGateway，避免事务回滚后仍推送。

- [ ] **Step 4: 接入业务事件**

私聊、新投递、简历查看、状态更新、面试、完整简历请求、投诉申诉结果和强退都调用 `NotificationEventService.create(...)`。

- [ ] **Step 5: 提供 App 增量同步接口**

`GET /api/mobile/notifications?afterEventId=&limit=100` 返回当前身份事件；`POST /read` 幂等标记已读；`POST /push-token` 注册安装实例 Token。

- [ ] **Step 6: 测试并提交**

Run: `mvn test`

```bash
git commit -am "feat: add durable identity-scoped notification events"
```

---

### Task 7: Android 强提醒、互踢和后台补偿

**Files:**
- Modify: `android-app/app/build.gradle.kts`
- Modify: `android-app/app/src/main/AndroidManifest.xml`
- Create: `android-app/app/src/main/java/com/weib/app/data/realtime/RealtimeClient.kt`
- Create: `android-app/app/src/main/java/com/weib/app/data/notification/NotificationSyncWorker.kt`
- Create: `android-app/app/src/main/java/com/weib/app/data/notification/WeibMessagingService.kt`
- Create: `android-app/app/src/main/java/com/weib/app/data/notification/SystemNotificationFactory.kt`
- Modify: `android-app/app/src/main/java/com/weib/app/data/AppRepository.kt`
- Modify: `android-app/app/src/main/java/com/weib/app/AppViewModel.kt`
- Test: `android-app/app/src/test/java/com/weib/app/NotificationDeduplicatorTest.kt`
- Test: `android-app/app/src/test/java/com/weib/app/ForcedLogoutReducerTest.kt`

- [ ] **Step 1: 写 eventId 去重和强退状态失败测试**

```kotlin
@Test fun duplicateEventOnlyNotifiesOnce() {
    assertTrue(deduplicator.accept("evt-1"))
    assertFalse(deduplicator.accept("evt-1"))
}

@Test fun kickedClearsSessionAndShowsSecurityMessage() {
    val next = reducer.reduce(loggedIn, SecurityEvent(reason = "KICKED"))
    assertNull(next.token)
    assertEquals("你的账号已在其他设备登录，请检查密码是否泄露；如有风险，请立即修改密码。", next.dialog)
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `android-app/gradlew testProdDebugUnitTest`

- [ ] **Step 3: 实现前台 WebSocket 和全局 401 拦截**

连接参数携带当前 Token 和 sid；收到 `FORCE_LOGOUT` 或 HTTP `401 KICKED` 时只执行一次退出流程。

- [ ] **Step 4: 实现 Android 通知渠道**

创建安全提醒、高优先级消息和普通业务三个 Channel；Android 13+ 请求 `POST_NOTIFICATIONS`。通知 deep link 定位聊天、投递、面试或申诉页面。

- [ ] **Step 5: 接入 FCM 可选配置和 WorkManager 降级**

存在 `google-services.json` 时启用 Firebase 插件与 MessagingService；缺少配置时构建仍成功，WorkManager 每 15 分钟增量拉取并显示未处理事件。

- [ ] **Step 6: 构建并提交**

Run: `android-app/gradlew lintProdDebug testProdDebugUnitTest assembleProdDebug`

```bash
git commit -am "feat: add android realtime and background notifications"
```

---

### Task 8: 补齐求职者 Android 完整业务

**Files:**
- Create: `android-app/app/src/main/java/com/weib/app/feature/seeker/jobs/**`
- Create: `android-app/app/src/main/java/com/weib/app/feature/seeker/applications/**`
- Create: `android-app/app/src/main/java/com/weib/app/feature/seeker/resume/**`
- Modify: `android-app/app/src/main/java/com/weib/app/data/WeibApi.kt`
- Modify: `src/main/java/com/weib/controller/mobile/MobileSeekerActionController.java`
- Test: `android-app/app/src/test/java/com/weib/app/feature/seeker/SeekerViewModelTest.kt`

- [ ] **Step 1: 写求职者状态机失败测试**

覆盖筛选分页、收藏切换、投递幂等、允许撤回状态、上传失败重试、公开简历开关和 403 错误展示。

- [ ] **Step 2: 运行测试确认失败**

Run: `android-app/gradlew testProdDebugUnitTest --tests "*SeekerViewModelTest"`

- [ ] **Step 3: 实现职位和投递完整页面**

列表、筛选、详情、公司、收藏、投递、撤回和面试信息均调用 JSON API；写请求自动携带 UUID `Idempotency-Key`。

- [ ] **Step 4: 实现简历编辑和系统文件选择器**

使用 Photo Picker/Storage Access Framework 选择头像和附件，显示缩略图、文件名、大小、上传进度和失败重试。增加期望职位、城市与公开状态。

- [ ] **Step 5: 测试并提交**

Run: `android-app/gradlew testProdDebugUnitTest assembleProdDebug`

```bash
git commit -am "feat: complete seeker android workflows"
```

---

### Task 9: 补齐 Boss Android 完整业务

**Files:**
- Create: `android-app/app/src/main/java/com/weib/app/feature/boss/dashboard/**`
- Create: `android-app/app/src/main/java/com/weib/app/feature/boss/company/**`
- Create: `android-app/app/src/main/java/com/weib/app/feature/boss/jobs/**`
- Create: `android-app/app/src/main/java/com/weib/app/feature/boss/applications/**`
- Modify: `src/main/java/com/weib/controller/mobile/MobileBossController.java`
- Test: `android-app/app/src/test/java/com/weib/app/feature/boss/BossViewModelTest.kt`

- [ ] **Step 1: 写 Boss 权限和页面状态失败测试**

覆盖公司未入驻、审核中、审核拒绝、可发布、发布禁令、职位编辑归属、投递状态和面试时间校验。

- [ ] **Step 2: 运行测试确认失败**

Run: `android-app/gradlew testProdDebugUnitTest --tests "*BossViewModelTest"`

- [ ] **Step 3: 实现公司、职位和投递页面**

工作台统计、公司编辑、职位 CRUD/关闭/重开/统计、投递筛选、简历详情、状态更新和面试安排全部接入真实接口。

- [ ] **Step 4: 为未开通权限和未审核公司提供明确引导**

`ROLE_NOT_ENABLED` 显示企业权限申请；`COMPANY_NOT_APPROVED` 显示审核状态与原因，不显示不可用操作按钮。

- [ ] **Step 5: 测试并提交**

Run: `android-app/gradlew testProdDebugUnitTest assembleProdDebug`

```bash
git commit -am "feat: complete boss android workflows"
```

---

### Task 10: 补齐聊天、论坛、投诉、申诉与上传

**Files:**
- Create: `android-app/app/src/main/java/com/weib/app/feature/chat/**`
- Create: `android-app/app/src/main/java/com/weib/app/feature/forum/**`
- Create: `android-app/app/src/main/java/com/weib/app/feature/moderation/**`
- Modify: `src/main/java/com/weib/controller/ChatController.java`
- Modify: `src/main/java/com/weib/controller/ForumController.java`
- Modify: `src/main/java/com/weib/controller/ComplaintController.java`
- Modify: `src/main/java/com/weib/controller/AppealController.java`
- Test: `android-app/app/src/test/java/com/weib/app/feature/chat/ChatViewModelTest.kt`
- Test: `android-app/app/src/test/java/com/weib/app/feature/forum/ForumViewModelTest.kt`
- Test: `android-app/app/src/test/java/com/weib/app/feature/moderation/ModerationViewModelTest.kt`

- [ ] **Step 1: 写公共功能失败测试**

覆盖消息分页/去重/已读、图片和文件上传、论坛搜索/发帖/评论/喜欢/收藏、投诉证据和封禁申诉。

- [ ] **Step 2: 运行测试确认失败**

Run: `android-app/gradlew testProdDebugUnitTest`

- [ ] **Step 3: 实现聊天和附件**

会话列表、历史分页、实时新消息、发送状态、重试、附件选择和安全下载；消息显示当前身份头像和时间。

- [ ] **Step 4: 实现论坛完整互动**

板块、搜索、帖子详情、Photo Picker 多图发布、标签、评论、喜欢和收藏；访客只能读取，发布和互动必须登录。

- [ ] **Step 5: 实现投诉和申诉**

目标选择、类别、描述、证据上传、记录列表和处理结果；被封禁账号只能访问登录、申诉和必要上传接口。

- [ ] **Step 6: 测试并提交**

Run: `mvn test`  
Run: `android-app/gradlew lintProdDebug testProdDebugUnitTest assembleProdDebug`

```bash
git commit -am "feat: complete android chat forum complaints and appeals"
```

---

### Task 11: 实现在线简历搜索和完整简历授权

**Files:**
- Create: `src/main/resources/db/V20260712__resume_access.sql`
- Create: `src/main/java/com/weib/entity/ResumeAccessRequest.java`
- Create: `src/main/java/com/weib/entity/ResumeAccessGrant.java`
- Create: `src/main/java/com/weib/entity/ResumeViewLog.java`
- Create: `src/main/java/com/weib/repository/ResumeAccessRequestRepository.java`
- Create: `src/main/java/com/weib/repository/ResumeAccessGrantRepository.java`
- Create: `src/main/java/com/weib/repository/ResumeViewLogRepository.java`
- Create: `src/main/java/com/weib/resumeaccess/ResumeDiscoveryService.java`
- Create: `src/main/java/com/weib/resumeaccess/ResumeConsentService.java`
- Create: `src/main/java/com/weib/controller/mobile/MobileResumeDiscoveryController.java`
- Create: `android-app/app/src/main/java/com/weib/app/feature/boss/talent/**`
- Create: `android-app/app/src/main/java/com/weib/app/feature/seeker/consent/**`
- Test: `src/test/java/com/weib/resumeaccess/ResumeConsentServiceTest.java`
- Test: `src/test/java/com/weib/resumeaccess/ResumePrivacyIntegrationTest.java`

- [ ] **Step 1: 写隐私和越权失败测试**

验证 PRIVATE 不可搜索；PUBLIC_SUMMARY 不含电话/邮箱/完整经历；未审核公司不可搜索；授权只对指定 Boss 身份有效；撤回后不可继续查看；每次完整查看产生日志。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -Dtest=ResumeConsentServiceTest,ResumePrivacyIntegrationTest test`

- [ ] **Step 3: 实现搜索和脱敏 DTO**

搜索仅返回显式字段 DTO，禁止序列化 `Resume` 实体后再删除敏感字段。查询按职位、技能、城市、学历、工作年限分页并限制最大 size=50。

- [ ] **Step 4: 实现请求、同意、拒绝、撤回和审计**

请求必须带该 Boss 公司下有效职位与说明；同意时保存简历版本并发送聊天卡片；完整查看在返回数据前写 `resume_view_logs`。

- [ ] **Step 5: 实现 Android 人才库和授权页面**

Boss 人才卡只显示公开摘要；求职者收到高优先级请求卡，可查看公司、职位和理由后决定。

- [ ] **Step 6: 测试并提交**

Run: `mvn test`  
Run: `android-app/gradlew testProdDebugUnitTest assembleProdDebug`

```bash
git commit -am "feat: add consent-based online resume discovery"
```

---

### Task 12: 更新接口文档、迁移工具与可观测性

**Files:**
- Modify: `docs/API_TESTING.md`
- Modify: `docs/RUNTIME_PROFILES.md`
- Create: `docs/ANDROID_TESTING.md`
- Modify: `src/main/java/com/weib/config/SwaggerConfig.java`
- Create: `scripts/verify_dual_role_migration.py`
- Create: `scripts/verify_session_slots.py`

- [ ] **Step 1: 发布完整测试契约**

记录 `selectedRole`、错误 reason、互踢场景、身份权限矩阵、通知增量游标、简历脱敏字段、请求示例和 Idempotency-Key。

- [ ] **Step 2: 编写只读验证脚本**

迁移脚本输出每个账号的角色集合并断言 `seeker_ahua`、`boss_*` 和普通 seeker 规则；Redis 脚本使用 SCAN 检查 WEB/MOBILE 槽位，不输出 Token 值。

- [ ] **Step 3: 增加结构化日志**

记录 `eventId`、`userId`、`activeRole`、`clientType`、`sidHash` 和结果；不记录密码、验证码、完整 JWT、电话、邮箱或简历内容。

- [ ] **Step 4: 验证并提交**

Run: `python scripts/verify_dual_role_migration.py --help`  
Run: `python scripts/verify_session_slots.py --help`  
Run: `mvn test`

```bash
git commit -am "docs: publish dual-role and android testing contract"
```

---

### Task 13: 全链路验证、主分支合并、部署和 ADB 安装

**Files:**
- Build output: `target/weib-1.0.0.jar`
- Build output: `android-app/app/build/outputs/apk/prod/debug/app-prod-debug.apk`

- [ ] **Step 1: 完整自动化验证**

Run: `mvn clean test package`  
Expected: BUILD SUCCESS，无失败测试。

Run: `cd admin-frontend && npm run build`  
Expected: TypeScript 与 Vite 构建成功。

Run: `android-app/gradlew lintProdDebug testProdDebugUnitTest assembleProdDebug`  
Expected: lint、单元测试与 APK 构建成功。

- [ ] **Step 2: 本地 ADB 验证**

```powershell
adb kill-server
adb start-server
adb devices -l
adb install -r android-app/app/build/outputs/apk/prod/debug/app-prod-debug.apk
adb shell am start -n com.weib.app/.MainActivity
```

验证普通 seeker、仅 Boss、双角色账号、同端互踢、通知、完整简历授权和所有补齐页面。

- [ ] **Step 3: 备份生产数据库和 JAR**

在服务器生成时间戳备份，记录 SHA-256；迁移前检查 MySQL、Redis、nginx 和 `weib.service` 状态。

- [ ] **Step 4: 先部署兼容数据库迁移，再部署 JAR**

重复执行迁移验证幂等；替换 `/opt/weib/weib-1.0.0.jar`，重启 `weib.service`，确认 prod profile 和 8888 端口。

- [ ] **Step 5: 公网端到端验证**

使用真实验证码分别验证：

- `seeker_ahua + SEEKER/BOSS` 均成功且数据隔离。
- 普通 seeker 选择 BOSS 返回 `ROLE_NOT_ENABLED`。
- `boss_zhang` 选择 SEEKER 返回 `ROLE_NOT_ENABLED`。
- 同账号第二个 MOBILE 登录使旧 MOBILE 返回 `KICKED`，WEB 保持在线；WEB 场景反向验证。
- 聊天、简历查看、通知、论坛、投诉、申诉和在线简历授权成功。

- [ ] **Step 6: 合并并推送主分支**

只有所有验证通过后 fast-forward 合并 `master` 并推送。将最终 APK 复制到 `C:\Users\cg\Desktop\weib-app-prod-debug.apk`，再次通过 ADB 覆盖安装并启动。

---

## Verification

- 后端：`mvn clean test package`
- 管理后台：`admin-frontend/npm run build`
- Android：`android-app/gradlew lintProdDebug testProdDebugUnitTest assembleProdDebug`
- 数据迁移：重复执行无重复角色、无身份丢失。
- 安全：旧 sid 被踢后不可访问；SEEKER/BOSS 消息和业务数据不可互查。
- 隐私：公开摘要不返回敏感字段，完整简历必须有有效授权并写审计。
- 生产：systemd active、Redis PONG、nginx 200、登录和 WebSocket 正常、日志无新增 ERROR。

**Next skill:** `$superpower-executing-plans`
