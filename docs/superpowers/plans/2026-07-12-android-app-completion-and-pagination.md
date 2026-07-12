# Android App Completion and Pagination Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use $superpower-subagents (recommended) or $superpower-executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking via update_plan.

**Goal:** Deliver a production-usable seeker/boss Android client with paginated cached lists, complete recruitment workflows, messaging/notifications, forum, complaints and appeals.

**Architecture:** Keep the Spring Boot database as source of truth, expose bounded page APIs, and cache hot read pages with the existing Redis cache-aside infrastructure. Split Android state into reusable paging logic and feature-focused Compose screens, while retaining the current Retrofit/session/realtime stack.

**Tech Stack:** Java 17, Spring Boot, Spring Data JPA, Redis, JUnit/MockMvc, Kotlin, Jetpack Compose, Retrofit/OkHttp, WorkManager, DataStore.

---

## File structure

- `src/main/java/com/weib/controller/SeekerApiController.java`: bounded searchable job pages and job detail.
- `src/main/java/com/weib/controller/mobile/MobileBossController.java`: boss job, application and talent pages.
- `src/main/java/com/weib/controller/mobile/MobileMessagingController.java`: mobile conversations, messages and resume requests.
- `src/main/java/com/weib/controller/ForumController.java`: paginated posts/comments and interactions.
- `src/main/java/com/weib/controller/ComplaintController.java`, `AppealController.java`: user moderation flows.
- `src/main/java/com/weib/service/*`: transactional business rules, notification events and cache invalidation.
- `android-app/app/src/main/java/com/weib/app/data/Paging.kt`: platform-independent paging reducer.
- `android-app/app/src/main/java/com/weib/app/data/WeibApi.kt`: complete API contract.
- `android-app/app/src/main/java/com/weib/app/data/AppRepository.kt`: feature operations and auth handling.
- `android-app/app/src/main/java/com/weib/app/AppViewModel.kt`: screen state and action orchestration.
- `android-app/app/src/main/java/com/weib/app/ui/screens/*`: feature-focused Compose screens.
- `android-app/app/src/main/java/com/weib/app/data/notification/*`: realtime/poll dedup and strong notifications.

### Task 1: Close session-expiry behavior

**Files:**
- Modify: `android-app/app/src/main/java/com/weib/app/data/AppRepository.kt`
- Modify: `android-app/app/src/main/java/com/weib/app/data/realtime/ForcedLogoutReducer.kt`
- Test: `android-app/app/src/test/java/com/weib/app/ForcedLogoutReducerTest.kt`

- [ ] Add a failing reducer test asserting `SESSION_EXPIRED` removes the token and provides a login message.
- [ ] Run `gradlew testProdDebugUnitTest --tests com.weib.app.ForcedLogoutReducerTest` and verify RED.
- [ ] Emit `SESSION_EXPIRED` only when a request carrying a local token receives non-kicked 401; add the reducer branch.
- [ ] Run the focused test and full Android unit suite; verify GREEN.
- [ ] Commit only the session-expiry files.

### Task 2: Add reusable Android paging state

**Files:**
- Create: `android-app/app/src/main/java/com/weib/app/data/Paging.kt`
- Create: `android-app/app/src/test/java/com/weib/app/PagingReducerTest.kt`
- Modify: `android-app/app/src/main/java/com/weib/app/data/Models.kt`

- [ ] Write tests proving first page replaces data, next page appends unique IDs, duplicate loads are ignored, refresh resets page to zero, and a failed append preserves existing items.
- [ ] Run `PagingReducerTest`; expected failure because `PagingState` and `PagingReducer` do not exist.
- [ ] Implement `PagingState<T>(items,page,pageSize,totalPages,refreshing,appending,error)` and pure reducer events `StartRefresh`, `StartAppend`, `PageLoaded`, `PageFailed`.
- [ ] Run focused and full Android tests.
- [ ] Commit paging state and tests.

### Task 3: Bound and cache job/talent pages on the server

**Files:**
- Modify: `src/main/java/com/weib/controller/SeekerApiController.java`
- Modify: `src/main/java/com/weib/controller/mobile/MobileBossController.java`
- Modify: relevant job/resume service and repository classes discovered by the controllers.
- Test: `src/test/java/com/weib/controller/SeekerApiControllerTest.java`
- Test: `src/test/java/com/weib/controller/mobile/MobileBossControllerTest.java`

- [ ] Add MockMvc tests: default size 20, maximum 50, negative page normalized to zero, keyword/city forwarded, and a user without boss role receives 403 for talent search.
- [ ] Run the focused Maven tests and verify failing expectations against the current `size=50` mobile contract.
- [ ] Return `PageResponse` metadata consistently and add `/api/mobile/boss/talents?page=0&size=20&q=&city=` with public resume summaries only.
- [ ] Reuse existing Redis cache-aside conventions for query pages; include normalized filters/page/sort in keys and invalidate job/resume list namespaces after writes.
- [ ] Run focused tests and `mvn test`.
- [ ] Commit server paging and cache behavior.

### Task 4: Implement lazy job and talent lists in Android

**Files:**
- Modify: `android-app/app/src/main/java/com/weib/app/data/WeibApi.kt`
- Modify: `android-app/app/src/main/java/com/weib/app/data/AppRepository.kt`
- Modify: `android-app/app/src/main/java/com/weib/app/AppViewModel.kt`
- Create: `android-app/app/src/main/java/com/weib/app/ui/screens/JobScreens.kt`
- Create: `android-app/app/src/main/java/com/weib/app/ui/screens/TalentScreens.kt`
- Modify: `android-app/app/src/main/java/com/weib/app/ui/WeibApp.kt`
- Test: `android-app/app/src/test/java/com/weib/app/JobPagingContractTest.kt`

- [ ] Add contract tests asserting page size 20, query reset, stable-ID dedup and next-page guard.
- [ ] Change Retrofit methods to `jobs(page,size,q,city)` and `talents(page,size,q,city)`; remove hardcoded `?size=50`.
- [ ] Add view-model refresh/load-next/search actions with a single in-flight job per list and 300 ms search debounce.
- [ ] Trigger next page when the last visible item is within five items of the end; show separate refresh/append errors and `没有更多数据`.
- [ ] Run Android tests and assemble the APK.
- [ ] Commit lazy lists.

### Task 5: Complete seeker and boss recruitment workflows

**Files:**
- Modify: `src/main/java/com/weib/controller/mobile/MobileSeekerActionController.java`
- Modify: `src/main/java/com/weib/controller/mobile/MobileBossController.java`
- Modify: associated application/resume/job services.
- Modify: `android-app/app/src/main/java/com/weib/app/data/WeibApi.kt`
- Modify: `android-app/app/src/main/java/com/weib/app/data/AppRepository.kt`
- Create: `android-app/app/src/main/java/com/weib/app/ui/screens/ApplicationScreens.kt`
- Create: `android-app/app/src/main/java/com/weib/app/ui/screens/ResumeScreens.kt`
- Create: `android-app/app/src/main/java/com/weib/app/ui/screens/BossJobScreens.kt`
- Test: server controller tests and Android UI contract tests.

- [ ] Add failing tests for job detail, favorite idempotency, duplicate application 409, withdrawal rules, boss job CRUD, application status transitions, local file upload and role enforcement.
- [ ] Implement only missing server endpoints/rules; keep writes idempotent and publish notification events after committed state changes.
- [ ] Add full Android forms and actions for seeker resume, favorites/applications and boss jobs/application processing.
- [ ] Keep form data on network/upload failure and show server validation messages.
- [ ] Run focused tests, full Maven tests and Android tests.
- [ ] Commit recruitment workflows.

### Task 6: Complete messaging and resume authorization

**Files:**
- Create or modify: `src/main/java/com/weib/controller/mobile/MobileMessagingController.java`
- Modify: messaging and notification services/entities/repositories.
- Modify: `android-app/app/src/main/java/com/weib/app/data/WeibApi.kt`
- Create: `android-app/app/src/main/java/com/weib/app/ui/screens/MessageScreens.kt`
- Modify: realtime client and view model.
- Test: server messaging authorization tests and Android message reducer tests.

- [ ] Add failing tests that conversations are isolated by user and active role, bosses see partial resumes before authorization, seekers can approve/reject a request, and approval grants only the requesting boss access.
- [ ] Implement conversation/message pages, resume-request lifecycle and notification events.
- [ ] Implement Android conversation list/detail, send, resume request and approval/rejection actions.
- [ ] Verify role switching requires logout and no messages leak between roles.
- [ ] Run all relevant tests and commit.

### Task 7: Complete forum, complaints and appeals

**Files:**
- Modify: `src/main/java/com/weib/controller/ForumController.java`
- Modify: `src/main/java/com/weib/controller/ComplaintController.java`
- Modify: `src/main/java/com/weib/controller/AppealController.java`
- Modify: associated services.
- Modify: `android-app/app/src/main/java/com/weib/app/data/WeibApi.kt`
- Create: `android-app/app/src/main/java/com/weib/app/ui/screens/ForumScreens.kt`
- Create: `android-app/app/src/main/java/com/weib/app/ui/screens/ModerationScreens.kt`
- Test: server permission/paging tests and Android navigation tests.

- [ ] Add failing tests for forum post/comment paging, image upload limits, like/favorite idempotency, complaint target validation, duplicate complaint/appeal rules and sanction enforcement.
- [ ] Add paginated comments while preserving existing post page behavior; expose mine/detail/create/upload endpoints to mobile session auth.
- [ ] Build section/tag/search/post/detail/comment/like/favorite screens with Android file picker image upload.
- [ ] Build complaint and appeal forms, evidence upload and status lists.
- [ ] Verify muted/publish-banned/account-banned users are blocked server-side.
- [ ] Run full tests and commit.

### Task 8: Strong notifications, end-to-end verification and delivery

**Files:**
- Modify: `android-app/app/src/main/java/com/weib/app/data/notification/NotificationSyncWorker.kt`
- Modify: `android-app/app/src/main/java/com/weib/app/data/notification/SystemNotificationFactory.kt`
- Modify: `android-app/app/src/main/java/com/weib/app/data/realtime/RealtimeClient.kt`
- Modify: `android-app/app/src/main/AndroidManifest.xml`
- Test: `android-app/app/src/test/java/com/weib/app/NotificationDeduplicatorTest.kt`

- [ ] Add tests for realtime/poll event-ID dedup, event-to-channel mapping and 401 worker termination.
- [ ] Map chat, application viewed/status, resume request and moderation result events to high-priority notification channels and deep-link destinations.
- [ ] Run `mvn test`, `gradlew testProdDebugUnitTest`, and `gradlew assembleProdDebug`.
- [ ] Install with ADB, verify seeker/boss core flows and confirm logcat shows `size=20` followed by page 1 only after scrolling.
- [ ] Commit remaining delivery changes, merge to main, push, deploy server, and verify systemd/nginx/Redis/API health before reporting completion.

## Verification commands

```powershell
mvn test
Set-Location android-app
.\gradlew.bat testProdDebugUnitTest assembleProdDebug
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s 127.0.0.1:16384 install -r app\build\outputs\apk\prod\debug\app-prod-debug.apk
```

## Next skill

`$superpower-executing-plans` — execute inline in this session because the user requested uninterrupted continuation and the shared worktree already contains active changes.
