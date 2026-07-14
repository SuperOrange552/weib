# Concise Complete API Word Manual V2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use $superpower-subagents (recommended) or $superpower-executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking via update_plan.

**Goal:** Generate a new, concise Word-only API testing manual in the existing `docs` folder that covers every current MVC endpoint, labels Android interfaces clearly, and contains request data sufficient for Apifox/Postman testing without changing any old manual.

**Architecture:** Build a deterministic endpoint inventory from current controllers, enrich it with OpenAPI plus explicit authentication/form/WebSocket overrides, validate every endpoint card as structured data, and generate the DOCX from that catalog. Verify coverage by comparing Controller routes with the generated catalog and DOCX text, then render every Word page for visual QA.

**Tech Stack:** Python 3, standard library, `python-docx`, Spring MVC source scanning, OpenAPI JSON, OOXML, bundled document runtime, LibreOffice/Poppler rendering helpers.

---

## File map

- Create `scripts/api_word_v2_inventory.py`: extract normalized MVC routes and page/business classification from current Java controllers.
- Create `scripts/api_word_v2_catalog.py`: assign module, platform badge, function, auth contract, request sample, response sample, and variable dependencies to each route.
- Create `scripts/generate_api_word_v2.py`: render the validated catalog into the compact Word manual.
- Create `scripts/verify_api_word_v2.py`: compare code routes, catalog entries, App labels, Word text, and preservation hashes.
- Create `scripts/test_api_word_v2.py`: standard-library unit tests for inventory, catalog, and DOCX contracts.
- Create `docs/verification/api-word-v2-openapi.json`: production OpenAPI snapshot used during this generation.
- Create `docs/verification/api-word-v2-verification.json`: machine-readable coverage and validation evidence.
- Create `docs/微招系统接口测试手册-精简完整版-V2.docx`: only user-facing deliverable.
- Preserve without modification: `docs/API_TESTING.md`, `docs/API_TESTING_COMPLETE.md`, `docs/微招系统完整接口测试文档.docx`, and `C:\Users\cg\Desktop\weib-api-doc.md`.

---

### Task 1: Lock preservation hashes and endpoint inventory

**Files:**
- Create: `scripts/api_word_v2_inventory.py`
- Create: `scripts/test_api_word_v2.py`
- Create: `docs/verification/api-word-v2-openapi.json`
- Test: `scripts/test_api_word_v2.py`

- [ ] **Step 1: Record old-manual hashes and fetch current OpenAPI**

Run:

```powershell
$old = @(
  'docs/API_TESTING.md',
  'docs/API_TESTING_COMPLETE.md',
  'docs/微招系统完整接口测试文档.docx',
  'C:\Users\cg\Desktop\weib-api-doc.md'
)
$old | ForEach-Object {
  $item = Get-Item -LiteralPath $_
  [pscustomobject]@{ Path=$item.FullName; Length=$item.Length; LastWriteTime=$item.LastWriteTimeUtc; SHA256=(Get-FileHash -LiteralPath $_ -Algorithm SHA256).Hash }
} | ConvertTo-Json -Depth 3 | Set-Content docs/verification/api-word-v2-preservation-before.json -Encoding UTF8
Invoke-WebRequest -UseBasicParsing http://superorange.top/v3/api-docs -OutFile docs/verification/api-word-v2-openapi.json
```

Expected: four old-file records are written and the OpenAPI snapshot parses as JSON with a non-empty `paths` object.

- [ ] **Step 2: Write failing inventory tests**

Create `scripts/test_api_word_v2.py` with:

```python
import json
import unittest
from pathlib import Path

from api_word_v2_inventory import extract_endpoints

ROOT = Path(__file__).resolve().parents[1]


class ApiWordV2InventoryTest(unittest.TestCase):
    def test_current_controllers_have_large_complete_inventory(self):
        endpoints = extract_endpoints(ROOT / "src/main/java/com/weib/controller")
        keys = {(item.method, item.path) for item in endpoints}
        self.assertGreaterEqual(len(endpoints), 158)
        self.assertIn(("POST", "/login"), keys)
        self.assertIn(("POST", "/api/mobile/auth/login"), keys)
        self.assertIn(("GET", "/api/admin/identities/users/{userId}"), keys)
        self.assertIn(("POST", "/api/forum/media"), keys)

    def test_inventory_has_no_duplicate_method_path(self):
        endpoints = extract_endpoints(ROOT / "src/main/java/com/weib/controller")
        keys = [(item.method, item.path) for item in endpoints]
        self.assertEqual(len(keys), len(set(keys)))


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 3: Run the test and verify RED**

Run:

```powershell
python -m unittest scripts/test_api_word_v2.py -v
```

Expected: import failure because `api_word_v2_inventory.py` does not exist.

- [ ] **Step 4: Implement normalized endpoint extraction**

Implement `Endpoint(method, path, controller, handler, page_route)` and functions that strip Java comments, read class-level `@RequestMapping`, expand multi-value mappings, normalize slashes, and identify page handlers. Reuse the proven annotation rules from `scripts/verify_api_practice_docs.py`, but return records rather than sets:

```python
@dataclass(frozen=True, order=True)
class Endpoint:
    method: str
    path: str
    controller: str
    handler: str
    page_route: bool


def normalize_path(value: str) -> str:
    value = re.sub(r"/+", "/", value.strip())
    if not value:
        return "/"
    if not value.startswith("/"):
        value = "/" + value
    return value[:-1] if len(value) > 1 and value.endswith("/") else value


def extract_endpoints(controller_root: Path) -> list[Endpoint]:
    values: dict[tuple[str, str], Endpoint] = {}
    for source in controller_root.rglob("*Controller.java"):
        text = strip_comments(source.read_text(encoding="utf-8"))
        prefix = class_prefix(text)
        for mapping in iter_method_mappings(text):
            for suffix in mapping.paths:
                path = normalize_path(prefix + suffix)
                endpoint = Endpoint(mapping.method, path, source.stem, mapping.handler, mapping.page_route)
                values[(endpoint.method, endpoint.path)] = endpoint
    return sorted(values.values())
```

- [ ] **Step 5: Verify GREEN and commit**

Run:

```powershell
python -m unittest scripts/test_api_word_v2.py -v
git add scripts/api_word_v2_inventory.py scripts/test_api_word_v2.py docs/verification/api-word-v2-openapi.json docs/verification/api-word-v2-preservation-before.json
git commit -m "docs(api): inventory current API routes"
```

Expected: inventory tests pass and the committed inventory source is current.

---

### Task 2: Build a complete, testable interface catalog

**Files:**
- Create: `scripts/api_word_v2_catalog.py`
- Modify: `scripts/test_api_word_v2.py`
- Test: `scripts/test_api_word_v2.py`

- [ ] **Step 1: Add failing catalog completeness and App-label tests**

Append tests that require one catalog card per inventory route and enforce the platform rule:

```python
from api_word_v2_catalog import build_catalog


class ApiWordV2CatalogTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.endpoints = extract_endpoints(ROOT / "src/main/java/com/weib/controller")
        cls.catalog = build_catalog(cls.endpoints, ROOT / "docs/verification/api-word-v2-openapi.json")

    def test_every_route_has_exactly_one_card(self):
        endpoint_keys = {(item.method, item.path) for item in self.endpoints}
        card_keys = {(item.method, item.path) for item in self.catalog}
        self.assertEqual(endpoint_keys, card_keys)
        self.assertEqual(len(self.catalog), len(card_keys))

    def test_mobile_routes_are_all_and_only_app_specific(self):
        for card in self.catalog:
            self.assertEqual(card.platform == "📱 APP 专用", card.path.startswith("/api/mobile/"))

    def test_every_business_card_is_directly_testable(self):
        for card in self.catalog:
            if card.page_route:
                continue
            self.assertTrue(card.function.strip(), card.key)
            self.assertTrue(card.full_url.startswith("http://superorange.top/"), card.key)
            self.assertTrue(card.permission.strip(), card.key)
            self.assertTrue(card.content_type.strip(), card.key)
            self.assertTrue(card.request_sample.strip(), card.key)
            self.assertTrue(card.success_response.strip(), card.key)
            self.assertNotIn("按实际填写", card.request_sample)
            self.assertNotIn("同上", card.request_sample)
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
python -m unittest scripts/test_api_word_v2.py -v
```

Expected: import failure because `api_word_v2_catalog.py` does not exist.

- [ ] **Step 3: Implement catalog types and deterministic classification**

Create `InterfaceCard` with fixed fields:

```python
@dataclass(frozen=True)
class InterfaceCard:
    code: str
    platform: str
    module: str
    method: str
    path: str
    full_url: str
    function: str
    permission: str
    content_type: str
    headers: tuple[tuple[str, str], ...]
    parameters: tuple[ParameterRow, ...]
    request_sample: str
    success_response: str
    variable_source: str
    key_errors: tuple[str, ...]
    page_route: bool

    @property
    def key(self) -> str:
        return f"{self.method} {self.path}"
```

Apply platform precedence in this exact order:

```python
def platform_for(path: str, page_route: bool) -> str:
    if path.startswith("/api/mobile/"):
        return "📱 APP 专用"
    if path.startswith("/api/admin/") or path.startswith("/admin"):
        return "管理后台"
    if page_route:
        return "页面路由"
    if path.startswith(("/api/forum/", "/api/complaints", "/api/appeals")):
        return "Web/App 通用"
    return "Web 专用"
```

Module order must be `auth`, `seeker`, `boss`, `community`, `files`, `mobile`, `admin`, `pages`. Assign stable codes such as `AUTH-001`, `SEEKER-001`, `APP-AUTH-001`, and `ADMIN-001` after sorting by module, path, and method.

- [ ] **Step 4: Implement OpenAPI sample synthesis and explicit overrides**

Read `paths`, component schemas, query/path/header parameters, request body media types, and `200` response schemas. Generate concrete values by field semantics:

```python
SAMPLE_VALUES = {
    "username": "seeker_ahua",
    "password": "Weib@123456",
    "phone": "13900139999",
    "selectedRole": "SEEKER",
    "role": "seeker",
    "captcha": "{{captcha}}",
    "jobId": "{{jobId}}",
    "encodedId": "{{encodedJobId}}",
    "applicationId": "{{applicationId}}",
    "conversationId": "{{conversationId}}",
    "page": 0,
    "size": 20,
    "keyword": "Java",
    "city": "北京",
    "title": "[API测试] Java开发工程师",
    "content": "这是用于接口测试的内容，可在测试完成后删除。",
}
```

Define explicit overrides for form and security-sensitive flows: `GET /captcha`, `POST /login`, `POST /register`, `POST /logout`, `POST /user/change-password`, Boss company/job forms, Web chat upload/download, forum media, resume media, admin auth, mobile auth, idempotent mutations, and file downloads. Each override includes actual `Content-Type`, Cookie/CSRF/Bearer headers, complete form or JSON body, success shape, and variable acquisition path.

For endpoints without a body, use the exact string `请求体：无`. For page routes, use `请求参数：按路径或查询参数表；响应：HTML 页面` and do not fabricate JSON.

- [ ] **Step 5: Add workflow variables and safe test data**

Add a document-level variable catalog containing:

```python
DEFAULT_VARIABLES = {
    "baseUrl": "http://superorange.top",
    "seekerUsername": "seeker_ahua",
    "bossUsername": "boss_li",
    "testPassword": "Weib@123456",
    "adminUsername": "admin",
    "captcha": "从同一 Cookie Session 的验证码图片人工读取",
    "csrfToken": "从 GET /login 或 GET /register 的隐藏字段读取",
    "adminToken": "从 POST /api/admin/auth/login 的 data.token 读取",
    "mobileToken": "从 POST /api/mobile/auth/login 的 data.token 读取",
    "idempotencyKey": "每个新业务请求生成 UUID；重试同一请求复用原值",
}
```

The generator must not include SSH, database, Redis, or hashed-password values.

- [ ] **Step 6: Verify GREEN and commit**

Run:

```powershell
python -m unittest scripts/test_api_word_v2.py -v
git add scripts/api_word_v2_catalog.py scripts/test_api_word_v2.py
git commit -m "docs(api): build complete testable interface catalog"
```

Expected: all catalog tests pass, no route is missing, and every `/api/mobile/**` entry is App-labelled.

---

### Task 3: Generate the compact Word manual

**Files:**
- Create: `scripts/generate_api_word_v2.py`
- Modify: `scripts/test_api_word_v2.py`
- Create: `docs/微招系统接口测试手册-精简完整版-V2.docx`
- Test: `scripts/test_api_word_v2.py`

- [ ] **Step 1: Load bundled workspace dependencies and document preset**

Use `codex_app__load_workspace_dependencies`, then read:

```text
documents/references/design_presets.md
documents/references/header_templates.md
documents/tasks/create_edit.md
documents/tasks/verify_render.md
```

Resolve the `compact_reference_guide` preset into explicit page, font, spacing, color, table, header, and footer constants before creating the document.

- [ ] **Step 2: Add failing DOCX structure tests**

Add tests that generate a temporary DOCX and inspect it with `python-docx`:

```python
from docx import Document
from generate_api_word_v2 import generate_manual


class ApiWordV2DocxTest(unittest.TestCase):
    def test_generated_word_contains_every_interface_and_app_badge(self):
        output = ROOT / "target/api-word-v2-test.docx"
        cards = build_catalog(
            extract_endpoints(ROOT / "src/main/java/com/weib/controller"),
            ROOT / "docs/verification/api-word-v2-openapi.json",
        )
        generate_manual(cards, output)
        doc = Document(output)
        text = "\n".join(paragraph.text for paragraph in doc.paragraphs)
        for card in cards:
            self.assertIn(card.key, text)
        app_count = sum(1 for card in cards if card.platform == "📱 APP 专用")
        self.assertEqual(text.count("📱 APP 专用"), app_count + 1)
        self.assertIn("请求方式与完整地址", text)
        self.assertIn("可复制请求数据", text)

    def test_old_manual_is_not_the_output_target(self):
        output = ROOT / "docs/微招系统接口测试手册-精简完整版-V2.docx"
        self.assertNotEqual(output.name, "微招系统完整接口测试文档.docx")
```

- [ ] **Step 3: Run tests and verify RED**

Run:

```powershell
python -m unittest scripts/test_api_word_v2.py -v
```

Expected: import failure because `generate_api_word_v2.py` does not exist.

- [ ] **Step 4: Implement document styles and page furniture**

Create explicit styles for Title, Subtitle, Heading 1-3, body, code, interface heading, platform badges, parameter tables, and note callouts. Use Letter portrait with 1-inch margins, Arial/微软雅黑 body text, Consolas code, repeatable table headers, and no fixed row heights.

The generator must add:

```python
def add_header_footer(section, generated_date: str) -> None:
    header = section.header.paragraphs[0]
    header.text = "微招系统接口测试手册 V2"
    header.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    footer = section.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    footer.add_run(f"生成日期：{generated_date}    第 ")
    add_page_field(footer)
    footer.add_run(" 页")
```

Use a compact cover, generated contents field, quick-start variable table, module bands, and a final variable/status-code appendix.

- [ ] **Step 5: Implement interface card rendering**

For each card, render in this exact order:

```python
def add_interface_card(doc: Document, card: InterfaceCard) -> None:
    add_interface_heading(doc, f"{card.code}｜{card.platform}｜{card.key}", card.platform)
    add_key_value_table(doc, [
        ("功能", card.function),
        ("请求方式与完整地址", f"{card.method} {card.full_url}"),
        ("权限与前置条件", card.permission),
        ("Content-Type", card.content_type),
        ("请求头", format_headers(card.headers)),
    ])
    add_parameter_table(doc, card.parameters)
    add_code_block(doc, "可复制请求数据", card.request_sample)
    add_code_block(doc, "成功响应示例", card.success_response)
    add_note(doc, "变量来源", card.variable_source)
    add_error_list(doc, card.key_errors)
```

Add `keep_with_next` to the interface heading and the first metadata row. Split long JSON only at paragraph boundaries; do not place request and response into narrow table cells.

- [ ] **Step 6: Generate the real Word file and verify GREEN**

Run with the bundled Python runtime:

```powershell
python scripts/generate_api_word_v2.py --openapi docs/verification/api-word-v2-openapi.json --output 'docs/微招系统接口测试手册-精简完整版-V2.docx'
python -m unittest scripts/test_api_word_v2.py -v
```

Expected: the new DOCX exists in `docs`, old documents remain unchanged, and all DOCX structure tests pass.

- [ ] **Step 7: Commit generator and first complete document**

Run:

```powershell
git add scripts/generate_api_word_v2.py scripts/test_api_word_v2.py 'docs/微招系统接口测试手册-精简完整版-V2.docx'
git commit -m "docs(api): generate concise complete Word manual"
```

---

### Task 4: Add strict coverage and preservation verification

**Files:**
- Create: `scripts/verify_api_word_v2.py`
- Create: `docs/verification/api-word-v2-verification.json`
- Modify: `scripts/test_api_word_v2.py`

- [ ] **Step 1: Add a failing verification test**

Add:

```python
from verify_api_word_v2 import verify_manual


class ApiWordV2VerificationTest(unittest.TestCase):
    def test_real_manual_has_full_coverage_and_preserves_old_files(self):
        result = verify_manual(ROOT)
        self.assertEqual(result["status"], "PASS")
        self.assertEqual(result["missingEndpoints"], [])
        self.assertEqual(result["unknownEndpoints"], [])
        self.assertEqual(result["mislabelledAppEndpoints"], [])
        self.assertEqual(result["changedOldFiles"], [])
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
python -m unittest scripts/test_api_word_v2.py -v
```

Expected: import failure because `verify_api_word_v2.py` does not exist.

- [ ] **Step 3: Implement verification**

`verify_manual(root)` must:

1. Re-extract Controller endpoints.
2. Rebuild the catalog.
3. Extract text from the new DOCX.
4. Compare every `METHOD /path` key.
5. Verify all and only `/api/mobile/**` cards carry the App badge.
6. Require every non-page card to contain all fixed field labels.
7. Recompute the four old-document length, mtime, and SHA-256 records.
8. Write `docs/verification/api-word-v2-verification.json`.

Use the exact result shape:

```python
result = {
    "status": "PASS" if not any(failures) else "INCOMPLETE",
    "controllerEndpointCount": len(endpoints),
    "businessEndpointCount": sum(not item.page_route for item in endpoints),
    "pageEndpointCount": sum(item.page_route for item in endpoints),
    "documentedEndpointCount": len(documented_keys),
    "appEndpointCount": sum(item.path.startswith("/api/mobile/") for item in endpoints),
    "missingEndpoints": sorted(code_keys - documented_keys),
    "unknownEndpoints": sorted(documented_keys - code_keys),
    "mislabelledAppEndpoints": sorted(app_label_errors),
    "incompleteCards": sorted(incomplete_cards),
    "changedOldFiles": sorted(changed_old_files),
}
```

- [ ] **Step 4: Run verification and commit**

Run:

```powershell
python scripts/verify_api_word_v2.py
python -m unittest scripts/test_api_word_v2.py -v
git add scripts/verify_api_word_v2.py scripts/test_api_word_v2.py docs/verification/api-word-v2-verification.json
git commit -m "test(api-docs): verify Word manual coverage"
```

Expected: `status=PASS`, no missing/unknown/mislabelled/incomplete/changed entries.

---

### Task 5: Validate representative requests against production

**Files:**
- Modify: `docs/verification/api-word-v2-verification.json`
- Modify only if evidence exposes an error: catalog or generator files from earlier tasks

- [ ] **Step 1: Validate public and read-only request formats**

Execute the manual's exact formats for:

```http
GET http://superorange.top/api/forum/sections
GET http://superorange.top/api/forum/posts?page=0&size=2
GET http://superorange.top/api/seeker/jobs?page=0&size=2&keyword=Java
GET http://superorange.top/check-username?username=api_test_unused_20260714
```

Expected: HTTP success responses parse according to the documented response shapes.

- [ ] **Step 2: Validate Web login and App login chains**

For Web, use one Cookie jar for `GET /login`, credential-bound `GET /captcha`, and `POST /login`; extract the CSRF hidden input and read the generated captcha from the same Session. For App, call the documented captcha/login sequence and confirm the response supplies a usable mobile token and active role. Do not store captcha text, cookies, or tokens in Git evidence.

Expected: `seeker_ahua` authenticates as SEEKER, `boss_li` authenticates as BOSS, and mobile auth returns the documented success envelope.

- [ ] **Step 3: Validate authenticated representative reads**

Use temporary runtime variables to call:

```http
GET /api/seeker/resume
GET /api/seeker/applications?page=0&size=2
GET /api/mobile/auth/me
GET /api/mobile/boss/dashboard
POST /api/admin/auth/login
GET /api/admin/auth/me
```

Only record status, response field names, and pass/fail; never commit live tokens or secrets.

- [ ] **Step 4: Correct any factual drift and rerun all checks**

If a documented field or media type differs from production, correct the catalog override, regenerate the Word file, rerun unit tests and strict verification, and update the evidence with:

```json
{
  "productionSampleVerification": {
    "publicReads": "PASS",
    "webLogin": "PASS",
    "mobileLogin": "PASS",
    "authenticatedReads": "PASS",
    "checkedAt": "2026-07-14T00:00:00+08:00"
  }
}
```

Replace `checkedAt` with the real execution time generated by the verification script.

- [ ] **Step 5: Commit verified corrections/evidence**

Run:

```powershell
git add scripts/api_word_v2_catalog.py scripts/generate_api_word_v2.py 'docs/微招系统接口测试手册-精简完整版-V2.docx' docs/verification/api-word-v2-verification.json
git commit -m "docs(api): validate manual samples against production"
```

Skip the commit if production validation required no tracked changes.

---

### Task 6: Render, inspect, and finalize the Word artifact

**Files:**
- Modify: `docs/微招系统接口测试手册-精简完整版-V2.docx`
- Modify as needed: `scripts/generate_api_word_v2.py`
- Create temporary QA output outside Git: `%TEMP%\weib-api-word-v2-render`

- [ ] **Step 1: Run structural and accessibility audits**

Run using bundled document tooling:

```powershell
python 'C:\Users\cg\.codex\plugins\cache\openai-primary-runtime\documents\26.709.11516\skills\documents\scripts\heading_audit.py' 'docs/微招系统接口测试手册-精简完整版-V2.docx'
python 'C:\Users\cg\.codex\plugins\cache\openai-primary-runtime\documents\26.709.11516\skills\documents\scripts\section_audit.py' 'docs/微招系统接口测试手册-精简完整版-V2.docx'
python 'C:\Users\cg\.codex\plugins\cache\openai-primary-runtime\documents\26.709.11516\skills\documents\scripts\a11y_audit.py' 'docs/微招系统接口测试手册-精简完整版-V2.docx'
python 'C:\Users\cg\.codex\plugins\cache\openai-primary-runtime\documents\26.709.11516\skills\documents\scripts\table_geometry.py' 'docs/微招系统接口测试手册-精简完整版-V2.docx' --audit
```

Expected: no missing heading hierarchy, invalid section geometry, clipped fixed-height tables, or critical accessibility errors.

- [ ] **Step 2: Render all pages to PNG**

Run:

```powershell
$render = Join-Path $env:TEMP 'weib-api-word-v2-render'
Remove-Item -LiteralPath $render -Recurse -Force -ErrorAction SilentlyContinue
python 'C:\Users\cg\.codex\plugins\cache\openai-primary-runtime\documents\26.709.11516\skills\documents\render_docx.py' 'docs/微招系统接口测试手册-精简完整版-V2.docx' --output_dir $render --emit_pdf
```

Expected: one PNG per Word page plus a PDF, with no renderer errors.

- [ ] **Step 3: Inspect every rendered page**

Create contact sheets only for navigation, but open page PNGs at full resolution to check every page for:

- clipped Chinese or emoji App badges;
- broken JSON indentation or lost monospace formatting;
- interface headings orphaned at page bottoms;
- parameter tables missing repeated headers;
- narrow columns, boundary-hugging text, blank pages, or unexpected large gaps;
- footer overlap and incorrect page numbers.

Record the page count and inspected page range in `api-word-v2-verification.json`.

- [ ] **Step 4: Fix and re-render until clean**

Make style fixes in the generator, regenerate the DOCX, rerun unit/coverage/a11y/table audits, and render all pages again. Do not hand-edit the generated Word file because regeneration must remain deterministic.

- [ ] **Step 5: Final preservation, coverage, and Git verification**

Run:

```powershell
python scripts/verify_api_word_v2.py
python -m unittest scripts/test_api_word_v2.py -v
git diff --check
git status --short --branch
```

Expected: full PASS evidence, four old documents unchanged, and only intentional V2 files modified.

- [ ] **Step 6: Commit final QA and push master**

Run:

```powershell
git add scripts docs/verification 'docs/微招系统接口测试手册-精简完整版-V2.docx'
git commit -m "docs(api): finalize concise complete Word manual"
git push origin master
git fetch origin master
git rev-parse HEAD
git rev-parse origin/master
```

Expected: local `HEAD` equals `origin/master`. If no files changed after the previous commit, skip the final commit and only push existing commits.

---

## Plan self-review

- Spec coverage: old-file preservation, Word-only delivery, full MVC coverage, page appendix, WebSocket section, fixed interface template, executable examples, App badges, live sample verification, structural audit, and page-by-page render QA all map to explicit tasks.
- Placeholder scan: no `TBD`, `TODO`, “implement later”, “similar to”, or undefined architecture decisions remain. Runtime timestamps and discovered endpoint totals are generated values, not deferred decisions.
- Type consistency: `Endpoint`, `InterfaceCard`, `ParameterRow`, `extract_endpoints`, `build_catalog`, `generate_manual`, and `verify_manual` are named consistently across tests and implementation tasks.
