# 微招 Android 双角色 App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use $superpower-subagents (recommended) or $superpower-executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking via update_plan.

**Goal:** 构建参考微招 Web 端视觉、只包含求职者和招聘者两种身份的原生 Android App，并补齐可直接调用的移动端 JSON 接口。

**Architecture:** Spring Boot 继续作为唯一业务和数据源，新增薄层移动端 Controller 复用现有 Service；Android 使用 Kotlin、Jetpack Compose、MVVM 和 Repository。一个 APK 根据登录返回角色切换导航，管理员接口和页面完全排除。

**Tech Stack:** Java 17 / Spring Boot 3.2 / JUnit 5；Kotlin / Jetpack Compose / Material 3 / Navigation Compose / Retrofit / OkHttp / Kotlin Serialization / DataStore / Coil；Gradle Kotlin DSL。

---

### Task 1: 固化移动端认证契约

**Files:**
- Create: `src/main/java/com/weib/dto/mobile/MobileLoginRequest.java`
- Create: `src/main/java/com/weib/dto/mobile/MobileLoginResponse.java`
- Create: `src/main/java/com/weib/controller/mobile/MobileAuthController.java`
- Modify: `src/main/java/com/weib/config/WebConfig.java`
- Test: `src/test/java/com/weib/controller/mobile/MobileAuthControllerTest.java`

- [ ] 先写 MockMvc 测试，覆盖验证码错误、密码错误、seeker/boss 成功和 admin 拒绝。
- [ ] 运行 `mvn -Dtest=MobileAuthControllerTest test`，确认测试先失败。
- [ ] 实现 JSON 登录，返回 `accessToken/expiresIn/user`，并创建同一用户 Session。
- [ ] 将 `/api/mobile/auth/login` 设为公开，将 `/api/mobile/**` 从表单 CSRF 校验中排除，其他移动接口仍由 LoginInterceptor + Bearer 保护。
- [ ] 重跑测试并提交 `feat: add mobile user authentication api`。

### Task 2: 补齐双角色 JSON 业务接口

**Files:**
- Create: `src/main/java/com/weib/controller/mobile/MobileSeekerActionController.java`
- Create: `src/main/java/com/weib/controller/mobile/MobileBossController.java`
- Create: `src/main/java/com/weib/dto/mobile/*.java`
- Test: `src/test/java/com/weib/controller/mobile/MobileRoleApiTest.java`

- [ ] 写角色和资源归属测试：求职者不能访问 Boss 接口，Boss 不能操作其他公司的职位或投递。
- [ ] 补齐求职者投递、撤回、收藏、取消收藏和简历上传 JSON 接口。
- [ ] 为 Boss 提供工作台、公司资料、职位 CRUD、投递列表、简历、状态流转、面试安排和统计 JSON 接口。
- [ ] 所有 ID 使用现有 `IdObfuscator`，写接口复用 Service 并返回统一 `Result`。
- [ ] 运行 `mvn test` 并提交 `feat: add seeker and boss mobile apis`。

### Task 3: 创建 Android 工程和设计系统

**Files:**
- Create: `android-app/settings.gradle.kts`
- Create: `android-app/build.gradle.kts`
- Create: `android-app/app/build.gradle.kts`
- Create: `android-app/app/src/main/AndroidManifest.xml`
- Create: `android-app/app/src/main/java/com/weib/app/ui/theme/*.kt`
- Create: `android-app/app/src/main/res/xml/network_security_config.xml`
- Test: `android-app/app/src/test/java/com/weib/app/ui/theme/WeibThemeTest.kt`

- [ ] 配置 `local`/`prod` flavors、Compose、Retrofit、DataStore、Coil 和 Navigation。
- [ ] 先写颜色和角色导航单元测试，再实现 Web 色板对应的 Compose Theme。
- [ ] 明文 HTTP 仅允许 `superorange.top`；本地 HTTPS 仅在 debug 构建使用调试信任策略。
- [ ] 运行 `android-app/gradlew testProdDebugUnitTest` 并提交 `feat: scaffold android app design system`。

### Task 4: 实现认证、验证码和会话

**Files:**
- Create: `android-app/app/src/main/java/com/weib/app/data/auth/*.kt`
- Create: `android-app/app/src/main/java/com/weib/app/feature/auth/*.kt`
- Test: `android-app/app/src/test/java/com/weib/app/feature/auth/LoginViewModelTest.kt`

- [ ] 测试验证码 120 秒倒计时、刷新冷却、空账号密码校验、角色路由和 401 清理会话。
- [ ] 使用持久 CookieJar 保证验证码与登录同一 Session，JWT 保存到 DataStore 并由 OkHttp 自动加入 Bearer Header。
- [ ] 登录页参考 Web 卡片样式，验证码显示倒计时，刷新按钮在冷却期禁用。
- [ ] 服务端返回 admin 时显示“App 仅支持求职者和招聘者”。
- [ ] 运行单元测试并提交 `feat: implement mobile login and captcha`。

### Task 5: 实现求职者端

**Files:**
- Create: `android-app/app/src/main/java/com/weib/app/feature/seeker/**/*.kt`
- Test: `android-app/app/src/test/java/com/weib/app/feature/seeker/*.kt`

- [ ] 实现职位列表/筛选、详情、公司详情、收藏和投递。
- [ ] 实现投递记录、撤回、面试信息、通知和收藏。
- [ ] 实现简历编辑、头像与附件系统选择器上传。
- [ ] 使用 `#0F172A/#334155` 显示标题和正文，并统一加载、空状态和错误重试。
- [ ] 运行测试并提交 `feat: implement seeker android experience`。

### Task 6: 实现招聘者端

**Files:**
- Create: `android-app/app/src/main/java/com/weib/app/feature/boss/**/*.kt`
- Test: `android-app/app/src/test/java/com/weib/app/feature/boss/*.kt`

- [ ] 实现工作台统计、公司审核状态和资料编辑。
- [ ] 实现职位发布、编辑、关闭、重新开放和统计。
- [ ] 实现投递筛选、简历详情、状态更新和面试安排。
- [ ] 保持与求职者端相同的主题、卡片、字号、间距和图标尺寸。
- [ ] 运行测试并提交 `feat: implement boss android experience`。

### Task 7: 接入公共社区与沟通功能

**Files:**
- Create: `android-app/app/src/main/java/com/weib/app/feature/forum/**/*.kt`
- Create: `android-app/app/src/main/java/com/weib/app/feature/chat/**/*.kt`
- Create: `android-app/app/src/main/java/com/weib/app/feature/moderation/**/*.kt`

- [ ] 接入论坛板块、搜索、详情、图文发布、评论、喜欢和收藏。
- [ ] 图片上传使用系统 Photo Picker，多图预览可删除，显示上传进度。
- [ ] 接入会话、消息轮询/实时刷新和安全附件下载。
- [ ] 接入投诉提交、记录和被封禁后的申诉提交；审核功能不进入 App。
- [ ] 提交 `feat: add forum chat complaint and appeal to android`。

### Task 8: 全链路验证、合并和部署

**Files:**
- Create: `android-app/README.md`
- Modify: `docs/API_TESTING.md`
- Modify: `docs/RUNTIME_PROFILES.md`

- [ ] 运行 `mvn test`，期望全部通过。
- [ ] 运行 `gradlew testProdDebugUnitTest assembleProdDebug`，期望生成可安装 APK。
- [ ] 使用模拟器或真机安装、启动并分别验证 seeker/boss 登录和核心接口。
- [ ] 使用 production APK 调用 `http://superorange.top`，确认验证码、登录、列表和写操作。
- [ ] 合并到 `master`、推送远端、打包部署后端，并用日志与公开端点复核。

## Verification

- 后端：`mvn test`
- Android：`android-app/gradlew lintProdDebug testProdDebugUnitTest assembleProdDebug`
- 安装：`adb install -r android-app/app/build/outputs/apk/prod/debug/app-prod-debug.apk`
- 服务器：验证码同 Session 登录、Bearer 调用 seeker/boss 接口、管理员账号拒绝进入 App。

**Next skill:** `$superpower-executing-plans`
