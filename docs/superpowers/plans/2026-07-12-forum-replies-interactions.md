# Forum Replies and Interaction Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use $superpower-subagents (recommended) or $superpower-executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking via update_plan.

**Goal:** Add production-ready two-level comment replies, visible like/favorite toggles, and a paginated “My Interactions” page for seeker and boss users.

**Architecture:** Extend `forum_comments` with nullable root-parent and direct-reply references, assemble a two-level response tree in `ForumService`, and keep existing mutation endpoints. Add current-user state to post responses and database-paginated liked/favorited queries ordered by interaction time. Thymeleaf pages remain shells while JavaScript renders through the existing Session and CSRF contract.

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Data JPA, Thymeleaf, vanilla JavaScript/CSS, MySQL 8, Maven/JUnit 5/Mockito.

---

## File map

- Create `src/main/resources/db/V20260712_08__forum_replies_interactions.sql` for the backward-compatible migration.
- Modify `ForumComment`, `ForumCommentCreateRequest`, and `ForumCommentResponse` for reply relationships.
- Modify forum repositories, `ForumService`, and `ForumController` for reply trees and personal interaction pages.
- Extend `ForumPostResponse` with current-user like/favorite state.
- Modify `forum-detail.html`, `forum-detail.js`, and `forum.css` for visible toggles and two-level replies.
- Create `forum-me.html` and `forum-me.js` and map `/forum/me`.
- Add migration, service, repository, access-policy, and template regression tests.

---

### Task 1: Backward-compatible reply schema

**Files:**
- Create: `src/main/resources/db/V20260712_08__forum_replies_interactions.sql`
- Modify: `src/main/java/com/weib/entity/ForumComment.java`
- Modify: `src/main/java/com/weib/repository/ForumCommentRepository.java`
- Test: `src/test/java/com/weib/integration/ForumReplyMigrationScriptTest.java`

- [ ] **Step 1: Write the failing migration contract test**

```java
@Test
void migrationAddsNullableReplyRelationshipsAndIndexes() throws Exception {
    String sql = resource("/db/V20260712_08__forum_replies_interactions.sql");
    assertThat(sql).contains("parent_id", "reply_to_comment_id");
    assertThat(sql).contains("idx_forum_comments_parent", "idx_forum_comments_reply_target");
    assertThat(sql).contains("ADD COLUMN");
}
```

- [ ] **Step 2: Run the test and verify RED**

```powershell
mvn -q '-Dtest=ForumReplyMigrationScriptTest' test
```

Expected: FAIL because the migration resource does not exist.

- [ ] **Step 3: Add repeatable migration and entity fields**

Add nullable `parent_id` and `reply_to_comment_id`, indexes, and foreign keys. Make the MySQL 8 SQL repeatable with `information_schema` checks and prepared statements. Add nullable `Long parentId` and `Long replyToCommentId` fields to the entity. Old rows remain root comments without rewriting data.

- [ ] **Step 4: Verify GREEN and commit**

```powershell
mvn -q '-Dtest=ForumReplyMigrationScriptTest' test
git add src/main/resources/db/V20260712_08__forum_replies_interactions.sql src/main/java/com/weib/entity/ForumComment.java src/main/java/com/weib/repository/ForumCommentRepository.java src/test/java/com/weib/integration/ForumReplyMigrationScriptTest.java
git commit -m "feat(forum): add two-level reply schema"
```

---

### Task 2: Two-level reply domain behavior

**Files:**
- Modify: `src/main/java/com/weib/dto/forum/ForumCommentCreateRequest.java`
- Modify: `src/main/java/com/weib/dto/forum/ForumCommentResponse.java`
- Modify: `src/main/java/com/weib/service/ForumService.java`
- Modify: `src/main/java/com/weib/controller/ForumController.java`
- Test: `src/test/java/com/weib/service/ForumServiceTest.java`

- [ ] **Step 1: Add failing service tests**

Exercise:

```java
service.comment(userId, "SEEKER", postId, new ForumCommentCreateRequest("一级评论", null));
service.comment(userId, "SEEKER", postId, new ForumCommentCreateRequest("回复一级", rootId));
service.comment(userId, "SEEKER", postId, new ForumCommentCreateRequest("回复追评", childId));
```

Assert root relationships are null; reply-to-root uses root for both IDs; reply-to-child keeps `parentId=rootId` and `replyToCommentId=childId`; cross-post, hidden, and missing targets fail; self-replies succeed; every created record increments `comment_count` once.

- [ ] **Step 2: Run tests and verify RED**

```powershell
mvn -q '-Dtest=ForumServiceTest' test
```

- [ ] **Step 3: Implement request, response, validation, and tree assembly**

Use:

```java
public record ForumCommentCreateRequest(String content, Long replyToCommentId) {}
```

Extend response with `parentId`, `replyToCommentId`, `replyToAuthorName`, and `List<ForumCommentResponse> replies`. Resolve a target only when active and in the same post. Compute root as `target.parentId == null ? target.id : target.parentId`. Build roots and children in two passes; child `replies` is always empty.

- [ ] **Step 4: Verify and commit**

```powershell
mvn -q '-Dtest=ForumServiceTest,ForumIdentityIsolationTest' test
git add src/main/java/com/weib/dto/forum/ForumCommentCreateRequest.java src/main/java/com/weib/dto/forum/ForumCommentResponse.java src/main/java/com/weib/service/ForumService.java src/main/java/com/weib/controller/ForumController.java src/test/java/com/weib/service/ForumServiceTest.java
git commit -m "feat(forum): support two-level comment replies"
```

---

### Task 3: Current-user state and personal interaction pagination

**Files:**
- Modify: `src/main/java/com/weib/dto/forum/ForumPostResponse.java`
- Modify: forum post/like/favorite repositories
- Modify: `src/main/java/com/weib/service/ForumService.java`
- Modify: `src/main/java/com/weib/controller/ForumController.java`
- Test: `src/test/java/com/weib/repository/ForumInteractionRepositoryTest.java`
- Test: `src/test/java/com/weib/service/ForumServiceTest.java`

- [ ] **Step 1: Add failing state and paging tests**

Prove authenticated detail exposes both booleans, anonymous detail returns both false, mutation remains idempotent, hidden posts are excluded, and personal pages are ordered by interaction creation time using database `Page` queries. Verify bounds `page >= 0` and `1 <= size <= 50`.

- [ ] **Step 2: Run tests and verify RED**

```powershell
mvn -q '-Dtest=ForumServiceTest,ForumInteractionRepositoryTest' test
```

- [ ] **Step 3: Implement repository queries, response flags, and APIs**

Add JPQL queries joining `ForumPost` to like/favorite rows and ordering by interaction `createdAt DESC`. Extend `ForumPostResponse` with `likedByCurrentUser` and `favoritedByCurrentUser`. Add an optional-user mapper and:

```http
GET /api/forum/me/likes?page=0&size=20
GET /api/forum/me/favorites?page=0&size=20
```

`/me` requires `ActiveIdentityResolver.current(session)`; public reads derive optional user ID without throwing for anonymous sessions.

- [ ] **Step 4: Verify and commit**

```powershell
mvn -q '-Dtest=ForumServiceTest,ForumInteractionRepositoryTest' test
git add src/main/java/com/weib/dto/forum/ForumPostResponse.java src/main/java/com/weib/repository src/main/java/com/weib/service/ForumService.java src/main/java/com/weib/controller/ForumController.java src/test/java/com/weib/repository/ForumInteractionRepositoryTest.java src/test/java/com/weib/service/ForumServiceTest.java
git commit -m "feat(forum): expose interaction state and personal lists"
```

---

### Task 4: Visible interaction controls and reply UI

**Files:**
- Modify: `src/main/resources/templates/forum-detail.html`
- Modify: `src/main/resources/static/js/forum-detail.js`
- Modify: `src/main/resources/static/css/forum.css`
- Test: `src/test/java/com/weib/integration/WebTemplateRegressionTest.java`

- [ ] **Step 1: Write failing asset-contract tests**

Assert assets include `forum-like-btn`, `forum-favorite-btn`, active-state CSS, POST/DELETE toggle logic, `replyToCommentId`, reply buttons for roots and children, inline “回复 @用户” composer, nested reply container, and accessible labels.

- [ ] **Step 2: Run test and verify RED**

```powershell
mvn -q '-Dtest=WebTemplateRegressionTest' test
```

- [ ] **Step 3: Implement buttons and two-level reply rendering**

Render real icon/text/count buttons with hover, focus, active, loading, and disabled states. Toggle with POST when inactive and DELETE when active; update state after successful JSON. Render roots as cards and children in an indented container. Only one inline reply composer is open at a time; preserve input on failure and reload the tree after success.

- [ ] **Step 4: Verify and commit**

```powershell
mvn -q '-Dtest=WebTemplateRegressionTest' test
git add src/main/resources/templates/forum-detail.html src/main/resources/static/js/forum-detail.js src/main/resources/static/css/forum.css src/test/java/com/weib/integration/WebTemplateRegressionTest.java
git commit -m "feat(forum): render replies and interaction toggles"
```

---

### Task 5: My Interactions visualization

**Files:**
- Modify: `src/main/java/com/weib/controller/ForumPageController.java`
- Create: `src/main/resources/templates/forum-me.html`
- Create: `src/main/resources/static/js/forum-me.js`
- Modify: `src/main/resources/static/css/forum.css`
- Modify: all three existing forum templates
- Test: `src/test/java/com/weib/integration/WebTemplateRegressionTest.java`
- Test: `src/test/java/com/weib/security/ForumAccessPolicyTest.java`

- [ ] **Step 1: Add failing route and UI tests**

Assert `/forum/me` is not public; the page has two summary cards, liked/favorited tabs, list, empty state, pagination, and cancel buttons; all forum nav bars link to `/forum/me`.

- [ ] **Step 2: Run tests and verify RED**

```powershell
mvn -q '-Dtest=WebTemplateRegressionTest,ForumAccessPolicyTest' test
```

- [ ] **Step 3: Implement page, tabs, paging, and cancel behavior**

Map `/forum/me`. Default to likes and load favorites on tab switch. Display totals from `PageResponse.totalElements`, render forum-style cards, cancel current interaction with DELETE, update count/list, and load the previous page when deletion empties the current page.

- [ ] **Step 4: Verify and commit**

```powershell
mvn -q '-Dtest=WebTemplateRegressionTest,ForumAccessPolicyTest' test
git add src/main/java/com/weib/controller/ForumPageController.java src/main/resources/templates/forum-me.html src/main/resources/static/js/forum-me.js src/main/resources/static/css/forum.css src/main/resources/templates/forum.html src/main/resources/templates/forum-detail.html src/main/resources/templates/forum-compose.html src/test/java/com/weib/integration/WebTemplateRegressionTest.java src/test/java/com/weib/security/ForumAccessPolicyTest.java
git commit -m "feat(forum): add my interactions dashboard"
```

---

### Task 6: Verification, deployment, and issue record

**Files:**
- Incrementally update: `C:\Users\cg\Desktop\新建文件夹\Boss直聘遇到的问题以及解决思路.md`

- [ ] **Step 1: Run pristine verification**

```powershell
mvn clean package
git diff --check
git status --short --branch
```

- [ ] **Step 2: Review full implementation**

Review for cross-post replies, anonymous state leakage, N+1 queries, counter drift, missing CSRF headers, and mobile-width regressions. Fix Critical/Important issues and rerun the full package.

- [ ] **Step 3: Deploy migration and JAR with backup**

```powershell
scp target/weib-1.0.0.jar ubuntu@110.40.157.142:/tmp/weib.jar
scp src/main/resources/db/V20260712_08__forum_replies_interactions.sql ubuntu@110.40.157.142:/tmp/forum-replies.sql
ssh ubuntu@110.40.157.142 "sudo /opt/weib/scripts/deploy.sh /tmp/weib.jar <timestamp> /tmp/forum-replies.sql"
```

- [ ] **Step 4: Execute production end-to-end verification**

With `seeker_lily` and `boss_li`: create root comment, reply to own root, reply to another reply, confirm two-level display and reply target, toggle like/favorite, confirm both `/forum/me` tabs, cancel both, inspect MySQL relationships/uniqueness, and check health, Redis, SHA, HTTP, and error logs.

- [ ] **Step 5: Update issue documentation and push**

Append a de-duplicated issue entry containing phenomenon, impact, root cause, design, actual changes, migration, verification results, release, and rollback path. Push `master`, then verify `HEAD == origin/master` and local JAR SHA equals production.

---

## Plan self-review

- Coverage includes two-level replies, direct targets, self-replies, visible toggles, liked/favorited dashboards, dual roles, DB paging, migration, deployment, rollback, and issue documentation.
- No deferred implementation placeholders remain; the deployment timestamp is generated at release time.
- `replyToCommentId`, `parentId`, `likedByCurrentUser`, and `favoritedByCurrentUser` are named consistently across layers.
