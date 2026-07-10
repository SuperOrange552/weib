# Complete API Practice Documentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use $superpower-subagents (recommended) or $superpower-executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking via update_plan.

**Goal:** Produce complete, source-verified Markdown and Word manuals that let a learner test every Weib business endpoint without consulting the frontend pages.

**Architecture:** Build one authoritative Markdown manual from the deployed OpenAPI plus current Controller/DTO/security source, then generate the Word document from that Markdown so both deliverables stay aligned. Add an automated verifier that compares documented method/path pairs against Controller mappings and performs structural/content checks; validate selected safe requests against `http://superorange.top` and visually render the DOCX before delivery.

**Tech Stack:** Java/Spring MVC source inspection, Python 3, `python-docx`, Markdown text processing, SpringDoc OpenAPI JSON, curl, PowerShell, Poppler/LibreOffice-based DOCX rendering where available.

---

## File Structure

- Create `docs/API_TESTING_COMPLETE.md`: authoritative human-readable interface manual.
- Create `docs/微招系统完整接口测试文档.docx`: formatted Word version generated from the Markdown manual.
- Create `scripts/verify_api_practice_docs.py`: extracts Controller mappings and checks documentation coverage and mandatory sections.
- Create `scripts/generate_api_practice_docx.py`: deterministic Markdown-to-DOCX converter for this manual's headings, tables, callouts and code blocks.
- Create `docs/verification/api-docs-verification.json`: machine-readable verification evidence and documented/implemented endpoint counts.
- Modify `README.md`: link to the new complete Markdown and Word manuals without removing the older historical link.

## Task 1: Build the Authoritative Endpoint Inventory

**Files:**
- Read: `src/main/java/com/weib/controller/**/*.java`
- Read: `src/main/java/com/weib/dto/**/*.java`
- Read: `src/main/java/com/weib/config/WebConfig.java`
- Read: `src/main/java/com/weib/config/CsrfInterceptor.java`
- Read: `src/main/java/com/weib/config/AdminSecurityConfig.java`
- Read: `src/main/java/com/weib/security/IdempotencyInterceptor.java`
- Read: live `http://superorange.top/v3/api-docs`
- Create: `scripts/verify_api_practice_docs.py`

- [ ] **Step 1: Save the current live OpenAPI snapshot outside the repository**

Run:

```powershell
curl.exe -sS http://superorange.top/v3/api-docs -o "$env:TEMP\weib-openapi-current.json"
python -m json.tool "$env:TEMP\weib-openapi-current.json" *> $null
```

Expected: curl exits `0`; JSON validation exits `0`; the snapshot contains `/api/admin/auth/login`.

- [ ] **Step 2: Extract Controller method/path pairs**

Implement `scripts/verify_api_practice_docs.py` with these responsibilities:

```python
CONTROLLER_ROOT = Path("src/main/java/com/weib/controller")
MAPPING_RE = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\s*\(\s*(?:value\s*=\s*)?"([^"]+)"')
DOC_ENDPOINT_RE = re.compile(r'^###\s+`(GET|POST|PUT|DELETE|PATCH)\s+([^`]+)`', re.MULTILINE)

def extract_controller_endpoints(root: Path) -> set[tuple[str, str]]:
    endpoints = set()
    for source in root.rglob("*Controller.java"):
        text = source.read_text(encoding="utf-8")
        class_prefix = ""
        class_match = re.search(r'@RequestMapping\s*\(\s*"([^"]+)"\s*\)\s*(?:public\s+)?class', text)
        if class_match:
            class_prefix = class_match.group(1)
        for match in MAPPING_RE.finditer(text):
            method, path = match.groups()
            endpoints.add((method.upper(), normalize_path(class_prefix + path)))
    return endpoints
```

The script must also handle mappings without parentheses such as `@GetMapping`, array-valued page mappings, and class-level prefixes. It must classify page-only GET handlers separately so they are allowed in the appendix rather than required in the main business API list.

- [ ] **Step 3: Add documentation contract checks**

The verifier must fail if any of these conditions is true:

```python
REQUIRED_TEXT = [
    "POST /login",
    "POST /api/admin/auth/login",
    "JSESSIONID",
    "_csrf",
    "X-Captcha-Expires-In",
    "Idempotency-Key",
    "Authorization: Bearer",
    "encodedId",
]
```

It must emit `docs/verification/api-docs-verification.json` containing:

```json
{
  "controllerEndpointCount": 0,
  "documentedMainEndpointCount": 0,
  "documentedAppendixEndpointCount": 0,
  "missingEndpoints": [],
  "unknownDocumentedEndpoints": [],
  "requiredTextMissing": [],
  "status": "PASS"
}
```

- [ ] **Step 4: Run the verifier before the manual exists**

Run:

```powershell
python scripts/verify_api_practice_docs.py
```

Expected: non-zero exit with a clear message that `docs/API_TESTING_COMPLETE.md` does not exist. This proves the coverage gate is active.

- [ ] **Step 5: Commit the verifier**

```powershell
git add scripts/verify_api_practice_docs.py
git commit -m "test: add API manual coverage verifier"
```

## Task 2: Write Common Rules and Both Login Workflows

**Files:**
- Create: `docs/API_TESTING_COMPLETE.md`
- Read: `src/main/java/com/weib/controller/UserController.java`
- Read: `src/main/java/com/weib/controller/CaptchaController.java`
- Read: `src/main/java/com/weib/service/CaptchaService.java`
- Read: `src/main/java/com/weib/controller/admin/AdminAuthController.java`
- Read: `src/main/java/com/weib/dto/admin/LoginRequest.java`

- [ ] **Step 1: Create the manual shell and navigation**

Create the title, document metadata, table of contents, base URL, variable table, common `Result<T>` format, HTML/redirect response explanation, authentication matrix and role matrix.

The first visible interface links must be:

```markdown
- 普通用户登录：[`POST /login`](#post-login)
- 管理员登录：[`POST /api/admin/auth/login`](#post-apiadminauthlogin)
```

- [ ] **Step 2: Document the ordinary-user login sequence**

Write four practice subsections in this exact order:

1. `GET /login` — save `JSESSIONID`, extract `_csrf`.
2. `GET /captcha` — reuse the same Cookie, display PNG, record the four-character value, observe 120-second TTL.
3. `POST /login` — `application/x-www-form-urlencoded` body with `username`, `password`, `captcha`, `_csrf`.
4. Verify the logged-in Session using a protected seeker or Boss request.

Include a copyable curl sequence using `-c`/`-b` cookie jar options and an Apifox/Postman checklist that does not require opening the website in a browser.

- [ ] **Step 3: Document administrator login**

Add `POST /api/admin/auth/login`, `GET /api/admin/auth/me`, and `POST /api/admin/auth/logout` with JSON examples. Show token extraction from `data.token` and the exact header:

```http
Authorization: Bearer <adminToken>
```

Explain that the login API can return HTTP 200 with a business `code` of 401 because the Controller wraps failures in `Result.error(401, ...)`; testers must assert both transport status and body code.

- [ ] **Step 4: Document shared validation, throttling, CSRF and idempotency rules**

Include username, password, phone, captcha, `Idempotency-Key`, `clientMessageId`, pagination, ISO-8601 time, `encodedId`, 302 redirects and 400/401/403/409/429 errors. Each rule must contain valid, invalid and boundary examples.

- [ ] **Step 5: Check the login section manually**

Run:

```powershell
Select-String docs/API_TESTING_COMPLETE.md -Pattern 'POST /login','POST /api/admin/auth/login','JSESSIONID','_csrf','captcha','Authorization: Bearer'
```

Expected: all six patterns appear in instructional prose and the relevant endpoint blocks.

- [ ] **Step 6: Commit the manual foundation**

```powershell
git add docs/API_TESTING_COMPLETE.md
git commit -m "docs: add complete authentication practice guide"
```

## Task 3: Document Public, Account and Seeker Endpoints

**Files:**
- Modify: `docs/API_TESTING_COMPLETE.md`
- Read: `src/main/java/com/weib/controller/UserController.java`
- Read: `src/main/java/com/weib/controller/SeekerApiController.java`
- Read: `src/main/java/com/weib/controller/JobController.java`
- Read: `src/main/java/com/weib/controller/ResumeController.java`
- Read: `src/main/java/com/weib/controller/MapController.java`

- [ ] **Step 1: Add public and account actions**

Document registration, username/phone availability check, logout and password change. Expand all form fields including `confirmPassword`, `role`, `_csrf` and `_idempotencyKey`/`Idempotency-Key`. Explicitly distinguish HTML responses from JSON responses.

- [ ] **Step 2: Add public seeker data queries**

Document job list, job detail and company detail, including every filter, sort option, zero-based page number, page size, salary range and `encodedId` source.

- [ ] **Step 3: Add authenticated seeker actions**

Document applications, favorites, apply, favorite toggle, withdraw, notifications and conversations. For each write action, state whether the actual endpoint requires Session, CSRF and `Idempotency-Key` based on `WebConfig` and `@Idempotent` rather than assuming all `/api` routes behave alike.

- [ ] **Step 4: Add both resume interfaces**

Document the JSON `GET/POST /api/seeker/resume` interface and the form `POST /resume/save` interface separately. Expand JSON fields:

```text
id, realName, gender, phone, email, birthday, education, school, major,
workExperience, projectExperience, skills, selfIntroduction
```

- [ ] **Step 5: Add geocoding**

Document `GET /api/geocode` with `address` and optional `city`, including expected failures when the upstream map service is unavailable.

- [ ] **Step 6: Run endpoint coverage and commit**

```powershell
python scripts/verify_api_practice_docs.py --allow-incomplete
git add docs/API_TESTING_COMPLETE.md docs/verification/api-docs-verification.json
git commit -m "docs: cover account and seeker APIs"
```

Expected: the verifier reports remaining Boss/chat/admin gaps but no missing documented-field requirements in the completed sections.

## Task 4: Document Boss, Company and Job Management Endpoints

**Files:**
- Modify: `docs/API_TESTING_COMPLETE.md`
- Read: `src/main/java/com/weib/controller/BossController.java`
- Read: `src/main/java/com/weib/controller/JobController.java`
- Read: `src/main/java/com/weib/entity/Company.java`
- Read: `src/main/java/com/weib/entity/Job.java`
- Read: `src/main/java/com/weib/entity/Application.java`

- [ ] **Step 1: Document company onboarding and editing**

Expand `name`, `industry`, `scale`, `address`, `description`, `contactName`, `contactPhone`, `contactEmail`, `longitude` and `latitude`. State which endpoint is idempotent and which currently is not annotated as idempotent.

- [ ] **Step 2: Document job creation, editing, deletion and reopening**

Expand `id`, `title`, `salaryMin`, `salaryMax`, `city`, `address`, `education`, `experience`, `description`, `requirements` and `tags`. Explain that `id` in the save form is a numeric internal job ID while route operations use `encodedId`, matching the current source.

- [ ] **Step 3: Document applicant handling**

Cover application list, resume lookup, status update, interview scheduling and job statistics. Expand the interview JSON body:

```json
{
  "interviewTime": "2026-07-15T14:30:00",
  "interviewLocation": "线上会议",
  "bossNote": "请提前准备项目介绍"
}
```

List accepted status values observed in the service/entity flow and mark any value not centrally validated as a current implementation characteristic.

- [ ] **Step 4: Run partial verification and commit**

```powershell
python scripts/verify_api_practice_docs.py --allow-incomplete
git add docs/API_TESTING_COMPLETE.md docs/verification/api-docs-verification.json
git commit -m "docs: cover boss and job management APIs"
```

## Task 5: Document Chat, File and Notification Endpoints

**Files:**
- Modify: `docs/API_TESTING_COMPLETE.md`
- Read: `src/main/java/com/weib/controller/ChatController.java`
- Read: `src/main/java/com/weib/controller/NotificationController.java`
- Read: `src/main/java/com/weib/entity/Message.java`

- [ ] **Step 1: Document file upload and protected download**

For upload, specify `multipart/form-data`, binary `file`, query/form `conversationId`, 10 MB current limit and allowed file rules from source. For download, explain `storedName`, Session ownership checks and 302/403/404 outcomes.

- [ ] **Step 2: Document message send and sync**

Expand send body fields:

```text
conversationId, receiverId, content, messageType, fileName, filePath,
fileSize, clientMessageId
```

Explain `text` versus `file`, participant validation, `sinceId`, `Idempotency-Key` and sender-scoped `clientMessageId` uniqueness.

- [ ] **Step 3: Document online state, read receipts and notifications**

Cover online status, `POST /api/chat/mark-read`, mark-one notification and mark-all notifications. Expand `conversationId` JSON and identify which notification calls require idempotency keys.

- [ ] **Step 4: Run partial verification and commit**

```powershell
python scripts/verify_api_practice_docs.py --allow-incomplete
git add docs/API_TESTING_COMPLETE.md docs/verification/api-docs-verification.json
git commit -m "docs: cover chat files and notifications"
```

## Task 6: Document Every Administrator Endpoint

**Files:**
- Modify: `docs/API_TESTING_COMPLETE.md`
- Read: `src/main/java/com/weib/controller/admin/*.java`
- Read: `src/main/java/com/weib/config/AdminSecurityConfig.java`
- Read: `src/main/java/com/weib/dto/admin/*.java`

- [ ] **Step 1: Add a role-permission matrix**

Record route families and required authorities exactly as configured:

```text
/api/admin/dashboard/**     ADMIN
/api/admin/companies/**     SUPER_ADMIN or AUDITOR
/api/admin/jobs/**          SUPER_ADMIN or AUDITOR
/api/admin/users/**         SUPER_ADMIN
/api/admin/admins/**        SUPER_ADMIN
/api/admin/audit-logs/**    SUPER_ADMIN or AUDITOR
/api/admin/export/**        SUPER_ADMIN or AUDITOR
```

- [ ] **Step 2: Document dashboard, audit and export queries**

Expand pagination, action, adminId, startDate, endDate, role, status, keyword and limit fields. Explain binary download handling for export responses.

- [ ] **Step 3: Document user, company and job management**

Cover list/detail/ban/unban/reset-password, list/detail/approve/reject and batch-offline. Expand JSON bodies `newPassword`, `reason` and `ids`; mark every `@Idempotent` write with the required header.

- [ ] **Step 4: Document sub-administrator management**

Cover list/create/change-role/disable. Expand `username`, `password`, `roleType`; include valid role types `super_admin`, `auditor`, `viewer` and the self-modification restriction.

- [ ] **Step 5: Run full endpoint verification**

```powershell
python scripts/verify_api_practice_docs.py
```

Expected: `status` is `PASS`, `missingEndpoints` is empty and the verification JSON contains non-zero main and appendix counts.

- [ ] **Step 6: Commit administrator coverage**

```powershell
git add docs/API_TESTING_COMPLETE.md docs/verification/api-docs-verification.json
git commit -m "docs: complete administrator API coverage"
```

## Task 7: Add the Page Appendix and Practice Test Catalogue

**Files:**
- Modify: `docs/API_TESTING_COMPLETE.md`
- Modify: `README.md`

- [ ] **Step 1: Add the page-only route appendix**

List GET routes that render HTML separately, with purpose, required role and expected redirect when unauthenticated. Do not give them the full API parameter template unless they accept meaningful input.

- [ ] **Step 2: Add practice cases**

Create ordered normal, invalid, boundary, authentication, authorization, CSRF, captcha, rate-limit, idempotency, pagination, ownership and file tests. Each case must name the endpoint, input change and expected outcome.

- [ ] **Step 3: Add a reusable variable worksheet**

Include placeholders for:

```text
baseUrl, JSESSIONID, csrfToken, captcha, userJwt, adminToken,
encodedJobId, encodedApplicationId, encodedCompanyId, conversationId,
receiverId, notificationId, adminUserId, idempotencyKey, clientMessageId
```

- [ ] **Step 4: Update README links**

Add links to both `docs/API_TESTING_COMPLETE.md` and `docs/微招系统完整接口测试文档.docx`, while labeling `docs/API_TESTING.md` as the older concise reference.

- [ ] **Step 5: Run final Markdown checks and commit**

```powershell
python scripts/verify_api_practice_docs.py
Select-String docs/API_TESTING_COMPLETE.md -Pattern 'TBD|TODO|待补充'
git diff --check
git add docs/API_TESTING_COMPLETE.md docs/verification/api-docs-verification.json README.md
git commit -m "docs: finalize complete API practice manual"
```

Expected: verifier PASS; placeholder search returns no matches; `git diff --check` returns no errors.

## Task 8: Generate and Visually Verify the Word Manual

**Files:**
- Create: `scripts/generate_api_practice_docx.py`
- Create: `docs/微招系统完整接口测试文档.docx`
- Read: `docs/API_TESTING_COMPLETE.md`

- [ ] **Step 1: Load the document runtime and implement the generator**

Use `python-docx` to create A4 pages with Chinese fonts, title page, document metadata, TOC field, heading hierarchy, repeated table headers, shaded code blocks, callout paragraphs, page breaks before major modules, header text and page-number footer.

The generator CLI must be:

```powershell
python scripts/generate_api_practice_docx.py `
  --input docs/API_TESTING_COMPLETE.md `
  --output docs/微招系统完整接口测试文档.docx
```

- [ ] **Step 2: Generate the DOCX twice and verify deterministic structure**

Run the generator twice, then use `python-docx` to assert:

```python
assert len(document.paragraphs) > 100
assert len(document.tables) > 20
assert any("POST /login" in p.text for p in document.paragraphs)
assert any("POST /api/admin/auth/login" in p.text for p in document.paragraphs)
```

Expected: both runs succeed and the structural assertions pass.

- [ ] **Step 3: Render the DOCX for visual QA**

Use the document skill's `render_docx.py` workflow to render every page to PNG, then create a contact sheet. Check title page, table widths, Chinese characters, code wrapping, page headers/footers and orphaned interface headings.

- [ ] **Step 4: Fix layout defects and re-render**

Adjust only the generator styles or pagination rules; regenerate the DOCX and repeat the render until no clipped tables, broken Chinese text or unreadable code blocks remain.

- [ ] **Step 5: Compare Markdown and Word endpoint titles**

Extract every ``### `METHOD /path` `` heading from Markdown and every matching heading paragraph from DOCX. Assert the two ordered lists are identical.

- [ ] **Step 6: Commit the generator and Word artifact**

```powershell
git add scripts/generate_api_practice_docx.py docs/微招系统完整接口测试文档.docx
git commit -m "docs: generate Word API practice manual"
```

## Task 9: Live-Safe Verification, Server Sync and Final Evidence

**Files:**
- Modify: `docs/verification/api-docs-verification.json`
- Read: `C:\Users\cg\Desktop\新建文件夹\openclaw.md`

- [ ] **Step 1: Verify safe live endpoints**

Use a temporary cookie jar to verify:

```powershell
curl.exe -sS -D - -o "$env:TEMP\weib-login.html" -c "$env:TEMP\weib-cookie.txt" http://superorange.top/login
curl.exe -sS -D - -o "$env:TEMP\weib-captcha.png" -b "$env:TEMP\weib-cookie.txt" http://superorange.top/captcha
curl.exe -sS http://superorange.top/v3/api-docs -o "$env:TEMP\weib-openapi-final.json"
```

Expected: login 200 with `JSESSIONID`; captcha 200 with PNG and `X-Captcha-Expires-In: 120`; OpenAPI valid JSON containing administrator login.

- [ ] **Step 2: Record hashes and final counts**

Update `docs/verification/api-docs-verification.json` with SHA-256 hashes of both deliverables, live OpenAPI operation count, controller endpoint count, documented main endpoint count, documented appendix count and timestamp.

- [ ] **Step 3: Copy both manuals to the server source tree**

Read the SSH connection data from `C:\Users\cg\Desktop\新建文件夹\openclaw.md` without printing secrets. Upload to:

```text
/opt/weib/source/docs/API_TESTING_COMPLETE.md
/opt/weib/source/docs/微招系统完整接口测试文档.docx
```

Do not restart `weib.service` because only documentation files changed.

- [ ] **Step 4: Verify remote hashes**

Compare local and remote SHA-256 hashes. Expected: both pairs match exactly.

- [ ] **Step 5: Run final repository verification**

```powershell
python scripts/verify_api_practice_docs.py
git diff --check
git status --short
```

Expected: verifier PASS; no whitespace errors; status only contains pre-existing unrelated user changes or intentionally uncommitted verification evidence.

- [ ] **Step 6: Commit final evidence**

```powershell
git add docs/verification/api-docs-verification.json
git commit -m "docs: record API manual verification evidence"
```

## Verification Summary

The implementation is complete only when all of the following are true:

- `docs/API_TESTING_COMPLETE.md` exists and passes the endpoint coverage verifier.
- `docs/微招系统完整接口测试文档.docx` exists, opens successfully and passes visual rendering QA.
- Both manuals contain ordinary-user and administrator login near the beginning.
- All request maps are expanded to concrete fields instead of undocumented generic objects.
- All Session, JWT, CSRF, captcha, role and idempotency requirements match current source.
- Word and Markdown endpoint heading lists are identical.
- Safe live checks match the documented login/captcha/OpenAPI behavior.
- Local and remote file hashes match after server synchronization.

**Next skill:** `$superpower-executing-plans` for inline execution in the current session.
