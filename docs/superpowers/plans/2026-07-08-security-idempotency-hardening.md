# Security and Idempotency Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use $superpower-subagents (recommended) or $superpower-executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking via update_plan.

**Goal:** Enforce consistent credential/input limits and prevent duplicate side effects across high-risk write operations.

**Architecture:** Centralize credential rules in `CredentialPolicy`; add an annotation-driven Redis `IdempotencyInterceptor`; keep database unique constraints as final protection; add a shared browser submit guard and explicit client message IDs.

**Tech Stack:** Java 17, Spring Boot 3.2, Jakarta Validation, Spring Data Redis, MySQL 8, JUnit 5/Mockito, Thymeleaf/JavaScript, React/TypeScript.

---

### Task 1: Credential policy

**Files:** create `CredentialPolicy.java`, `CredentialPolicyTest.java`; modify `User.java`, `UserService.java`, user/admin controllers and DTOs.

- [ ] Write failing boundary tests for username lengths 2/3/32/33 and password lengths 7/8/64/65, character classes, username/phone equality.
- [ ] Run targeted test and confirm RED.
- [ ] Implement trim/validation and make registration, change/reset password and admin creation reuse it.
- [ ] Add login maximum-length rejection before BCrypt work.
- [ ] Add Jakarta Validation to admin DTOs and `@Valid` controllers.
- [ ] Run targeted and full tests; commit.

### Task 2: Redis idempotency core

**Files:** create `Idempotent.java`, `IdempotencyService.java`, `IdempotencyInterceptor.java`, tests; modify `WebConfig.java`.

- [ ] Write failing tests for first acquisition, processing duplicate, completed duplicate, different keys, completion TTL and Redis failure degradation.
- [ ] Implement key validation (UUID/8–128 safe chars), `SET NX` 30-second lock, 10-minute completed marker and user/session+method+path scoping.
- [ ] Register interceptor after security/rate limiting and return safe JSON/HTTP 409 or duplicate success.
- [ ] Run tests and commit.

### Task 3: Apply idempotency and database safeguards

**Files:** modify job, boss, chat and admin controllers/services/entities/repositories; create `V20260708__security_idempotency.sql`.

- [ ] Annotate/apply idempotency to application, favorite, company, job, interview, admin creation, chat send and state endpoints.
- [ ] Make favorite an explicit target-state operation; duplicate application returns existing record semantics.
- [ ] Add `Message.clientMessageId` and sender/client ID unique constraint.
- [ ] Add safe MySQL migration checks for username width, password width and unique indexes; never truncate data.
- [ ] Add service tests for duplicate/domain-state behavior and run them.
- [ ] Commit.

### Task 4: Frontend constraints and submit guards

**Files:** create `static/js/submit-guard.js`; modify Thymeleaf forms/scripts and admin React API/actions.

- [ ] Add HTML minlength/maxlength/pattern/autocomplete and matching help text.
- [ ] Inject UUID idempotency keys into POST forms; disable submit buttons while pending and restore on pageshow.
- [ ] Wrap fetch writes to reuse a key for an in-flight identical request; add client message IDs.
- [ ] Ensure job apply/favorite, boss/admin actions and chat buttons visibly disable while pending.
- [ ] Build admin frontend and commit only source/generated assets expected by the project.

### Task 5: Verification, migration and deployment

- [ ] Run `mvn clean test package` and `npm run build` with zero failures.
- [ ] Run migration preflight on production MySQL; verify no username exceeds 32 before ALTER.
- [ ] Back up schema, apply migration, verify columns/indexes.
- [ ] Upload source/JAR, restart systemd and verify Redis/MySQL/application/Nginx.
- [ ] Exercise boundary inputs and duplicate requests online; inspect logs for errors and sensitive data.
- [ ] Complete requirement-by-requirement audit.