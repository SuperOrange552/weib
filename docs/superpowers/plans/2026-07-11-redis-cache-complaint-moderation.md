# Redis 缓存与投诉审核处罚 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `$superpower-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking via `update_plan`.

**Goal:** 为 Boss/求职者/公司/职位/简历增加生产级 Redis Cache-Aside 读取与一致性保护，并实现全对象投诉、后台审核、临时/永久处罚和审计通知闭环。

**Architecture:** MySQL 继续作为唯一持久化来源；公开读取统一经过 Cache-Aside 服务，采用 Redis 命中优先、单 Key 加载锁、空值缓存、TTL 抖动、数据库回源上限和事务提交后的延迟双删。投诉和处罚以独立表保存，处罚在登录、聊天、职位/简历写入及公开查询边界统一执行，管理端沿用现有 JWT/RBAC/幂等接口。

**Tech Stack:** Spring Boot 3.2.5、Java 17、Spring Data Redis/Lettuce、Spring Data JPA、MySQL 8、Thymeleaf/原生 JS、React 18 + TypeScript + MUI、JUnit 5/Mockito、现有 `audit_logs` 与通知服务。

---

## Task 1: 建立可回滚的数据库迁移和领域实体

**Files:**
- Create: `src/main/resources/db/V20260711__cache_complaints_sanctions.sql`
- Create: `src/main/java/com/weib/entity/Complaint.java`
- Create: `src/main/java/com/weib/entity/UserSanction.java`
- Create: `src/main/java/com/weib/repository/ComplaintRepository.java`
- Create: `src/main/java/com/weib/repository/UserSanctionRepository.java`
- Modify: `src/main/java/com/weib/entity/Job.java`, `Company.java`, `Resume.java` only when an explicit `visibilityStatus`/`auditStatus` field is required by the existing schema

- [ ] **Step 1: Write the failing repository/entity tests**

Add `src/test/java/com/weib/repository/ComplaintRepositoryTest.java` with an in-memory or mocked repository contract covering: one `PENDING` complaint per `(reporterId,targetType,targetId)`, status filtering, and active sanction lookup by user/type/time. Assert the intended enum/string values before the repositories exist.

- [ ] **Step 2: Run the focused tests and verify they fail for missing types**

Run:

```powershell
mvn -q -Dtest=ComplaintRepositoryTest test
```

Expected: compilation failure because `Complaint`, `UserSanction`, and repository methods are not yet present.

- [ ] **Step 3: Add the idempotent MySQL migration**

Create `complaints` with indexes on `(status,created_at)`, `(target_type,target_id)`, and `(reporter_id,target_type,target_id,status)`; create `user_sanctions` with indexes on `(user_id,sanction_type,status,starts_at,ends_at)` and `source_complaint_id`. Store evidence as bounded `TEXT`; use `VARCHAR` for enum-like values; use nullable `ends_at` for permanent sanctions. The script must use the project’s existing `IF NOT EXISTS`/procedure style and must not delete data.

- [ ] **Step 4: Add JPA entities and repository query methods**

Implement fields from the approved spec with `LocalDateTime` timestamps and `@PrePersist/@PreUpdate`. Add repository methods equivalent to:

```java
boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(Long reporterId, String targetType, Long targetId, String status);
Page<Complaint> findByStatus(String status, Pageable pageable);
Optional<UserSanction> findFirstByUserIdAndSanctionTypeAndStatusAndStartsAtLessThanEqualAndEndsAtGreaterThan(Long userId, String type, String status, LocalDateTime now, LocalDateTime now2);
List<UserSanction> findByUserIdAndStatus(Long userId, String status);
```

Use a separate query for permanent sanctions (`endsAt IS NULL`) rather than pretending `NULL` is greater than a timestamp.

- [ ] **Step 5: Run migration/entity tests and commit**

Run `mvn -q -Dtest=ComplaintRepositoryTest test`; expected PASS. Commit:

```powershell
git add src/main/resources/db/V20260711__cache_complaints_sanctions.sql src/main/java/com/weib/entity src/main/java/com/weib/repository src/test/java/com/weib/repository/ComplaintRepositoryTest.java
git commit -m "feat: add complaint and sanction persistence"
```

## Task 2: Implement cache-aside primitives and invalidation

**Files:**
- Create: `src/main/java/com/weib/cache/CacheAsideService.java`
- Create: `src/main/java/com/weib/cache/CacheInvalidationService.java`
- Create: `src/main/java/com/weib/cache/CacheKeys.java`
- Modify: `src/main/java/com/weib/config/RedisConfig.java`
- Modify: `src/main/resources/application.yml`
- Test: `src/test/java/com/weib/cache/CacheAsideServiceTest.java`, `CacheInvalidationServiceTest.java`

- [ ] **Step 1: Write failing cache tests**

Mock `StringRedisTemplate` and a loader `Supplier<T>` to prove: hit does not call loader; miss calls loader once and writes a TTL; concurrent miss uses a `SET NX EX` lock; missing value writes a 30-second marker; Redis exception falls back to loader; invalidation deletes immediately and schedules a second deletion.

- [ ] **Step 2: Run focused tests and verify failure**

Run `mvn -q -Dtest=CacheAsideServiceTest,CacheInvalidationServiceTest test`; expected compilation or assertion failures for missing classes.

- [ ] **Step 3: Implement stable key and TTL policy**

`CacheKeys` must generate `cache:user:public:{id}`, `cache:company:{id}`, `cache:job:{id}`, `cache:jobs:list:{sha256(query)}:{page}:{size}`, `cache:resume:public:{id}`, `cache:resume:user:{id}` and matching `lock:cache:load:*` keys. `CacheAsideService` must use bounded lock wait, TTL jitter, empty marker TTL, and no infinite retries. Redis values must use existing JSON serialization and safe DTOs.

- [ ] **Step 4: Implement delayed double delete**

`CacheInvalidationService.invalidate(String... keys)` deletes immediately and submits a scheduled task with an 800ms delay to delete the same keys again. Catch/log Redis errors without rolling back the database transaction. Configure the scheduler with a bounded pool in `WebConfig` or a focused `CacheTaskConfig`.

- [ ] **Step 5: Run tests, inspect TTL/key names, and commit**

Run focused tests and `mvn -q -DskipTests compile`; expected PASS. Commit cache primitives separately.

## Task 3: Route Boss/company/job/resume reads through Redis

**Files:**
- Modify: `src/main/java/com/weib/service/JobService.java`
- Modify: `src/main/java/com/weib/service/CompanyService.java`
- Modify: `src/main/java/com/weib/service/ResumeService.java`
- Modify: `src/main/java/com/weib/service/UserService.java`
- Modify: `src/main/java/com/weib/controller/IndexController.java`, `BossController.java`, `ResumeController.java`, `SeekerApiController.java` only where a query bypasses the service layer
- Test: `src/test/java/com/weib/service/CacheBackedReadServiceTest.java`

- [ ] **Step 1: Add failing service tests**

Verify the first `getJobById`, company lookup, resume lookup, and public user lookup load from MySQL once, while the second call returns the cached safe DTO/entity without repository access. Verify list queries include normalized filter/page/size in the cache key and never request more than 100 records.

- [ ] **Step 2: Implement cache-backed reads**

Use `CacheAsideService.get(key, type, loader, ttl)` in the service methods. Preserve existing transactional boundaries and ownership checks. Do not cache `PageImpl` or entities containing passwords; cache serializable public DTOs or reconstruct entities explicitly.

- [ ] **Step 3: Invalidate all related layers after writes**

After `create/update/delete` job/company/resume and user profile writes commit, invalidate detail, owner, company, list, and public-user keys. For a job, clear `job:{id}`, its company detail, and all job list namespaces; for a resume, clear both resume ID and user ID keys.

- [ ] **Step 4: Run service tests and existing test suite**

Run `mvn -q -Dtest=CacheBackedReadServiceTest,UserControllerCsrfTest,CaptchaServiceTest test`; expected PASS. Do not deploy until `mvn test` is green.

## Task 4: Add sanction checks at all business boundaries

**Files:**
- Create: `src/main/java/com/weib/service/SanctionService.java`
- Create: `src/main/java/com/weib/exception/SanctionDeniedException.java`
- Modify: `src/main/java/com/weib/service/UserService.java`
- Modify: `src/main/java/com/weib/controller/ChatController.java`
- Modify: `src/main/java/com/weib/controller/BossController.java`
- Modify: `src/main/java/com/weib/controller/ResumeController.java`
- Modify: `src/main/java/com/weib/controller/JobController.java`
- Modify: `src/main/java/com/weib/config/LoginInterceptor.java`
- Test: `src/test/java/com/weib/service/SanctionServiceTest.java`

- [ ] **Step 1: Write failing sanction tests**

Cover active temporary sanction, permanent sanction, expired sanction, revoked sanction, and independent MUTE/PUBLISH_BAN/ACCOUNT_BAN types. Assert that expired sanctions do not block and that Redis/cache invalidation is triggered after status changes.

- [ ] **Step 2: Implement sanction lookup with Redis caching**

`SanctionService.hasActive(userId,type)` checks a short-TTL Redis decision key, falls back to the repository, treats `endsAt == null` as permanent, and lazily marks expired rows `EXPIRED`. Cache invalidation must happen on create/revoke/expire.

- [ ] **Step 3: Enforce checks**

Reject login and authenticated requests for ACCOUNT_BAN; reject REST and WebSocket chat sends for MUTE; reject job create/edit/reopen and resume save/publish/avatar update for PUBLISH_BAN. Return existing `Result.error`/redirect conventions with a stable user-facing message and no sensitive sanction details.

- [ ] **Step 4: Run focused tests and commit**

Run `mvn -q -Dtest=SanctionServiceTest test`; expected PASS. Commit boundary enforcement separately.

## Task 5: Implement user complaint submission and evidence validation

**Files:**
- Create: `src/main/java/com/weib/dto/ComplaintCreateRequest.java`
- Create: `src/main/java/com/weib/dto/ComplaintResponse.java`
- Create: `src/main/java/com/weib/service/ComplaintService.java`
- Create: `src/main/java/com/weib/controller/ComplaintController.java`
- Modify: `src/main/java/com/weib/config/ApiExceptionHandler.java` or `GlobalExceptionHandler.java`
- Test: `src/test/java/com/weib/service/ComplaintServiceTest.java`

- [ ] **Step 1: Write failing complaint tests**

Cover valid USER/JOB/COMPANY/RESUME/MEDIA targets, self-report rejection, nonexistent target, duplicate pending complaint, description length, evidence count/type/size, unauthenticated request, and idempotent duplicate submission.

- [ ] **Step 2: Implement service validation and persistence**

Normalize target types/categories against a whitelist; sanitize description; validate evidence URLs against the project’s upload storage path; create one `PENDING` row; return an opaque complaint ID and status. Use the existing `Idempotent` mechanism on the write endpoint.

- [ ] **Step 3: Expose user endpoints**

Add `POST /api/complaints`, `GET /api/complaints/mine`, and `GET /api/complaints/{id}`. Use the logged-in session user, never trust `reporterId` from the request body. Return `401` when unauthenticated, `400` for validation, `409` for duplicate pending complaint, and `200` for accepted creation.

- [ ] **Step 4: Add Thymeleaf complaint UI**

Add a shared complaint modal fragment and buttons to job detail, company detail, resume preview, boss/seeker profile cards, and media areas. Submit with `fetch`, show validation errors and complaint number, and disable the submit button while the request is pending.

- [ ] **Step 5: Run tests and commit**

Run `mvn -q -Dtest=ComplaintServiceTest test`; expected PASS. Build frontend templates via `mvn -q -DskipTests package` after UI changes.

## Task 6: Implement admin complaint review and sanctions

**Files:**
- Create: `src/main/java/com/weib/dto/admin/ComplaintListResponse.java`
- Create: `src/main/java/com/weib/dto/admin/ComplaintReviewRequest.java`
- Create: `src/main/java/com/weib/dto/admin/SanctionCreateRequest.java`
- Create: `src/main/java/com/weib/controller/admin/AdminComplaintController.java`
- Create: `src/main/java/com/weib/service/admin/AdminComplaintService.java`
- Modify: `src/main/java/com/weib/config/AdminSecurityConfig.java`
- Modify: `src/main/java/com/weib/entity/AuditLog.java` comments/actions if required
- Test: `src/test/java/com/weib/service/admin/AdminComplaintServiceTest.java`

- [ ] **Step 1: Write failing admin service tests**

Cover paged filtering, reject, resolve with content offline, MUTE/PUBLISH_BAN/ACCOUNT_BAN creation, temporary `endsAt`, permanent `NULL`, revoke, audit log, notification, and cache invalidation. Assert that a second review of a non-PENDING complaint is rejected.

- [ ] **Step 2: Implement transactional review service**

`AdminComplaintService` must update complaint status and content/sanction records in one transaction, call `AuditLogService`, `NotificationService`, and `CacheInvalidationService`, and enforce permission: auditor may review ordinary complaints; super admin is required for permanent account bans and revocations.

- [ ] **Step 3: Add admin endpoints**

Implement:

```text
GET  /api/admin/complaints
GET  /api/admin/complaints/{id}
POST /api/admin/complaints/{id}/reject
POST /api/admin/complaints/{id}/resolve
POST /api/admin/sanctions
POST /api/admin/sanctions/{id}/revoke
GET  /api/admin/sanctions
```

All mutating endpoints use `@Idempotent`, request-body validation, existing `Result` format, JWT principal admin ID, and stable 400/403/404/409 responses.

- [ ] **Step 4: Extend admin RBAC**

Allow `/api/admin/complaints/**` to `AUDITOR`/`SUPER_ADMIN`; keep sanction revocation and permanent account ban protected by method-level `@PreAuthorize("hasRole('SUPER_ADMIN')")`.

- [ ] **Step 5: Run service tests and commit**

Run `mvn -q -Dtest=AdminComplaintServiceTest test`; expected PASS.

## Task 7: Build admin React complaint review UI

**Files:**
- Create: `admin-frontend/src/api/complaints.ts`
- Create: `admin-frontend/src/pages/ComplaintReviewPage.tsx`
- Create: `admin-frontend/src/types/complaints.ts`
- Modify: `admin-frontend/src/App.tsx`
- Modify: `admin-frontend/src/components/AdminSidebar.tsx`
- Modify: `admin-frontend/src/types/index.ts` only if shared role types are required

- [ ] **Step 1: Add typed API client methods**

Implement paged list/detail/reject/resolve/sanction/revoke calls using the existing Axios client; do not manually create JWT or idempotency headers because `client.ts` already injects them.

- [ ] **Step 2: Add review page**

Implement filters for status/target/category/date, a detail drawer with evidence preview, reject dialog, resolve dialog, sanction type selector, temporary duration or permanent option, reason field, loading/error states, and confirmation for account bans.

- [ ] **Step 3: Register route and role menu**

Register `/admin/complaints` in `App.tsx`; add a complaints menu item visible to auditor and super admin. Hide sanction-revoke controls for non-super-admin users.

- [ ] **Step 4: Build the admin frontend**

Run:

```powershell
Set-Location admin-frontend
npm run build
```

Expected: TypeScript and Vite build succeed with no errors. Copy the generated static assets through the existing project build/deploy process.

## Task 8: Add integration tests, observability, and API documentation

**Files:**
- Create: `src/test/java/com/weib/integration/ComplaintModerationIntegrationTest.java`
- Create: `src/test/java/com/weib/integration/CacheConsistencyIntegrationTest.java`
- Modify: `docs/API_TESTING_COMPLETE.md`
- Modify: `docs/微招系统完整接口测试文档.docx` through the existing document generation/verification workflow
- Modify: `src/main/resources/application.yml` with feature flags and logging levels

- [ ] **Step 1: Test end-to-end complaint lifecycle**

Use MockMvc and mocked Redis/MySQL boundaries to verify submit -> admin resolve -> sanction -> blocked action -> notification/audit -> revoke/expiry -> action allowed. Verify old records with null avatar remain readable.

- [ ] **Step 2: Test cache consistency and failure modes**

Verify write-after-read race, immediate and delayed deletes, Redis outage fallback, lock timeout, empty marker expiry, list invalidation, and no password/token serialization.

- [ ] **Step 3: Add production configuration**

Add environment-backed flags for cache-first reads, Redis fallback, complaint evidence size/count, lock wait, double-delete delay, and public read rate limits. Default to safe behavior in development and log structured cache hit/miss/fallback/sanction events without sensitive values.

- [ ] **Step 4: Update tester-facing API docs**

Document every new user/admin endpoint, request field, validation boundary, status code, `Idempotency-Key`, permissions, temporary/permanent sanction examples, Redis behavior, and old-data compatibility. Re-run the existing API-doc verification script and Word render/structure checks.

## Task 9: Verification, deployment, and rollback rehearsal

**Files:**
- No source additions; use existing scripts and deployment artifacts.

- [ ] **Step 1: Run all backend tests and static checks**

Run:

```powershell
mvn test
git diff --check
```

Expected: all tests pass, no whitespace errors.

- [ ] **Step 2: Build backend and admin frontend**

Run `mvn -q -DskipTests package` and `npm run build` under `admin-frontend`; record artifact paths and SHA-256 values.

- [ ] **Step 3: Apply migration on a backup/staging database**

Back up first, apply `V20260711__cache_complaints_sanctions.sql`, verify indexes and row counts, then test old null-avatar rows and new complaint/sanction rows.

- [ ] **Step 4: Deploy with feature flags disabled, then enable cache-first reads**

Upload source/JAR/static assets to the existing `/opt/weib` layout, restart `weib.service`, verify `systemctl is-active weib.service`, `redis-cli ping`, direct app port, Nginx domain, login, captcha, user reads, complaint submit/review, sanctions, logs, and Redis TTLs. Enable cache-first reads only after smoke tests pass.

- [ ] **Step 5: Record rollback evidence**

Verify that disabling cache-first reads keeps the site usable, that complaint history is retained, and that no migration rollback deletes user data. Save command output and artifact hashes under `docs/verification/`.

## Verification checklist

- [ ] `mvn test` passes with zero failures.
- [ ] Admin `npm run build` passes.
- [ ] Cache hit/miss/lock/TTL/double-delete tests pass.
- [ ] Complaint duplicate/self-report/evidence validation tests pass.
- [ ] Sanction enforcement tests cover login/chat/job/resume boundaries.
- [ ] Admin RBAC and idempotency tests pass.
- [ ] API Markdown and Word docs include all new endpoints and pass existing verification.
- [ ] Staging migration and old-data compatibility checks pass.
- [ ] Live Redis, systemd, Nginx, login, complaint, and sanction smoke tests pass before claiming deployment complete.
