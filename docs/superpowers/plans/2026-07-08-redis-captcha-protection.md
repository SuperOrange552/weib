# Redis Captcha Protection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use $superpower-subagents (recommended) or $superpower-executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking via update_plan.

**Goal:** Store login/register captchas in Redis for two minutes, show an auto-refreshing countdown, and prevent refresh abuse when credentials are empty or requests are too frequent.

**Architecture:** Add a focused `CaptchaService` responsible for Redis keys, TTL, refresh cooldown, IP limits, verification, failure counts, and Session fallback. Keep image generation in `CaptchaController`, delegate verification from `UserController`, and share a small browser script between the login and registration templates.

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Data Redis, MockMvc/JUnit 5, Thymeleaf, vanilla JavaScript.

---

## File map

- Create `src/main/java/com/weib/service/CaptchaService.java`: captcha lifecycle and Redis/fallback storage.
- Modify `src/main/java/com/weib/controller/CaptchaController.java`: image endpoint, client IP, headers and 429 responses.
- Modify `src/main/java/com/weib/controller/UserController.java`: use `CaptchaService` for login/register verification.
- Modify `src/main/java/com/weib/config/RateLimitInterceptor.java`: include method and URI in rate-limit keys.
- Create `src/main/resources/static/js/captcha.js`: credential gating, countdown, refresh and error UI.
- Modify `src/main/resources/templates/login.html`: disabled initial state and script wiring.
- Modify `src/main/resources/templates/register.html`: disabled initial state and script wiring.
- Create `src/test/java/com/weib/service/CaptchaServiceTest.java`: TTL, refresh, one-time verification, failures and fallback.
- Create `src/test/java/com/weib/config/RateLimitInterceptorTest.java`: endpoint-isolated rate keys.
- Create `src/test/java/com/weib/controller/CaptchaControllerTest.java`: response headers and 429 contract.

### Task 1: Captcha service tests and implementation

- [ ] Write `CaptchaServiceTest` with mocked `StringRedisTemplate` covering 120-second code TTL, 5-second cooldown, 10-per-60-second IP limit, overwrite/reset behavior, success deletion, fifth-failure invalidation, expiration, and Session fallback.
- [ ] Run `mvn -Dtest=CaptchaServiceTest test` and verify failure because `CaptchaService` does not exist.
- [ ] Implement `CaptchaService` with constants `CODE_TTL_SECONDS=120`, `COOLDOWN_SECONDS=5`, `RATE_WINDOW_SECONDS=60`, `RATE_MAX_REQUESTS=10`, and `MAX_FAILURES=5`.
- [ ] Use keys `captcha:code:{sessionId}`, `captcha:fail:{sessionId}`, `captcha:cooldown:{sessionId}`, and `captcha:rate:GET:/captcha:{ip}`; keep the fallback code and expiration timestamp in Session only when Redis throws.
- [ ] Return typed results for issuance and verification so controllers can distinguish valid, invalid, expired, locked and rate-limited states.
- [ ] Run `mvn -Dtest=CaptchaServiceTest test` and verify all service tests pass.
- [ ] Commit only the service and its test.

### Task 2: Controller integration and endpoint contract

- [ ] Write `CaptchaControllerTest` for PNG success with `X-Captcha-Expires-In: 120`, no-store headers, and JSON HTTP 429 containing `retryAfterSeconds`.
- [ ] Run `mvn -Dtest=CaptchaControllerTest test` and verify the new contract fails.
- [ ] Inject `CaptchaService` into `CaptchaController`, generate the image only after successful issuance, resolve IP from the first `X-Forwarded-For` address then `X-Real-IP` then `remoteAddr`, and return the specified headers.
- [ ] Inject `CaptchaService` into `UserController`; replace both static `CaptchaController.verify` calls with service verification and map expired/locked results to explicit Chinese messages.
- [ ] Run controller tests and targeted login/register tests.
- [ ] Commit controller integration without staging unrelated working-tree files.

### Task 3: Isolate generic rate-limit keys

- [ ] Write `RateLimitInterceptorTest` proving `/login` and `/register` from the same IP use different Redis keys.
- [ ] Run `mvn -Dtest=RateLimitInterceptorTest test` and verify it fails against the current IP-only key.
- [ ] Change the suffix to include `request.getMethod()` and `request.getRequestURI()` before the IP/user identity while preserving existing limits.
- [ ] Run the interceptor test and verify it passes.
- [ ] Commit only the interceptor and test while preserving pre-existing edits.

### Task 4: Shared frontend countdown and credential gating

- [ ] Create `captcha.js` exposing initialization via `data-*` selectors rather than duplicating logic.
- [ ] On page load show “请先输入账号和密码” and do not request `/captcha`.
- [ ] When both fields first become non-empty, fetch `/captcha` as a Blob, set the image object URL, clear the captcha input, and display `有效期 02:00`.
- [ ] Store an absolute deadline; update once per second; when it reaches zero automatically fetch a new captcha if both fields remain populated, otherwise disable the image and stop the timer.
- [ ] On manual click enforce local 5-second cooldown, while treating backend 429 and `retryAfterSeconds` as authoritative.
- [ ] Revoke old object URLs and clear intervals on unload to avoid leaks.
- [ ] Wire the shared script into `login.html` and `register.html`, removing direct `onclick` URL cache-busting.
- [ ] Manually verify empty-field gating, initial load, countdown, automatic refresh, input clearing and 429 messages in both pages.
- [ ] Commit the frontend files.

### Task 5: Full verification and deployment

- [ ] Run `npm run build` in `admin-frontend` and verify exit 0.
- [ ] Run `mvn clean test package` in the project root and verify exit 0 with zero failed tests.
- [ ] Start against Redis locally or in an isolated test profile and inspect `TTL captcha:code:*` to confirm a value near 120 seconds.
- [ ] Build the executable JAR, calculate SHA-256, upload the new source archive and JAR to `/opt/weib`, and preserve `/opt/weib/uploads` plus the MySQL database.
- [ ] Restart `weib.service`, wait for Spring Boot startup, and verify Redis reports `PONG`.
- [ ] Verify login and registration captcha endpoints, 5-second cooldown, 60-second IP limit, two-minute TTL, HTTPS root, `/admin/`, and the Nginx domain.
- [ ] Check `journalctl -u weib.service -p err` contains no new errors and report the deployed hashes.

## Verification

- Automated: `mvn clean test package` and `npm run build`.
- Behavioral: no captcha request with empty credentials; `02:00` countdown; automatic expiry refresh; refresh cooldown; per-IP rate limiting; five failures invalidate; successful captcha is one-time.
- Deployment: systemd active/enabled, Redis PONG, HTTP/HTTPS/domain 200, no error logs, matching artifact SHA-256.
