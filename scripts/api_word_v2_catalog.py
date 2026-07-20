#!/usr/bin/env python3
"""Build a complete test-oriented catalog for the Weib MVC inventory."""

from __future__ import annotations

import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable
from urllib.parse import urlencode

from api_word_v2_inventory import Endpoint


BASE_URL = "https://superorange.top"
APP_BADGE = "📱 APP 专用"

DEFAULT_VARIABLES = {
    "baseUrl": BASE_URL,
    "seekerUsername": "seeker_ahua",
    "bossUsername": "boss_li",
    "testPassword": "Weib@123456",
    "adminUsername": "admin",
    "captcha": "正常流程从 GET /captcha 图片读取；启用测试工具时可从 GET /api/test/captcha 的 data.captcha 读取",
    "testCaptchaAccessKey": "仅保存在测试环境变量 TEST_CAPTCHA_ACCESS_KEY 中，请求时放入 X-Test-Access-Key",
    "csrfToken": "从 GET /login 或 GET /register 返回 HTML 的隐藏字段 _csrf 读取",
    "adminToken": "从 POST /api/admin/auth/login 成功响应的 data.token 读取",
    "mobileToken": "从 POST /api/mobile/auth/login 成功响应的 data.accessToken 读取",
    "idempotencyKey": "每个新业务请求生成 UUID；重试同一请求时复用原值",
    "encodedJobId": "从职位列表/详情响应或页面链接取得经过编码的职位 ID",
    "jobId": "从职位列表、Boss 职位管理或创建职位成功响应取得",
    "applicationId": "从我的投递或 Boss 收到的简历列表取得",
    "postId": "从论坛帖子列表或发帖成功响应取得",
    "commentId": "从帖子详情的评论列表取得",
}


@dataclass(frozen=True)
class ParameterRow:
    name: str
    location: str
    required: str
    type: str
    example: str
    description: str


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


SAMPLE_VALUES: dict[str, Any] = {
    "username": "seeker_ahua", "password": "Weib@123456", "oldpassword": "Weib@123456",
    "newpassword": "Weib@654321", "confirmpassword": "Weib@654321", "phone": "13900139999",
    "email": "api_test@example.com", "nickname": "接口测试用户", "realname": "接口测试用户",
    "selectedrole": "SEEKER", "activerole": "SEEKER", "role": "SEEKER", "captcha": "{{captcha}}",
    "id": 1, "userid": 1, "jobid": "{{jobId}}", "encodedid": "{{encodedJobId}}",
    "applicationid": "{{applicationId}}", "conversationid": "{{conversationId}}", "messageid": 1,
    "postid": "{{postId}}", "commentid": "{{commentId}}", "parentid": "{{commentId}}",
    "sectionid": 1, "targetid": 1, "appealid": 1, "notificationid": 1, "aftereventid": 0,
    "page": 0, "size": 20, "limit": 20, "keyword": "Java", "q": "Java", "city": "北京",
    "title": "[API测试] Java开发工程师", "content": "这是用于接口测试的内容，可在测试后删除。",
    "description": "这是用于接口测试的说明，可在测试后删除。", "reason": "API 自动化测试",
    "category": "虚假信息", "targettype": "JOB", "type": "SYSTEM", "status": "PENDING",
    "education": "本科", "experience": "1-3年", "school": "测试大学", "major": "计算机科学",
    "skills": "Java,Spring Boot,MySQL", "salarymin": 12000, "salarymax": 22000,
    "address": "北京市海淀区中关村", "industry": "互联网", "scale": "100-499人",
    "name": "API测试科技有限公司", "companyname": "API测试科技有限公司",
    "contactname": "测试联系人", "contactphone": "13900139999", "contactemail": "api_test@example.com",
    "longitude": 116.397428, "latitude": 39.90923, "tags": "Java，后端，招聘",
    "installationid": "android-emulator-api-test", "pushtoken": "test-push-token-not-for-production",
    "devicename": "Codex Android Emulator", "clienttype": "MOBILE", "value": "API测试值",
}


FORM_OVERRIDES: dict[tuple[str, str], dict[str, Any]] = {
    ("POST", "/login"): {"fields": ["username", "password", "captcha", "selectedRole", "remember", "_csrf"]},
    ("POST", "/register"): {"fields": ["username", "password", "confirmPassword", "phone", "email", "nickname", "role", "captcha", "_csrf"]},
    ("POST", "/logout"): {"fields": ["_csrf"]},
    ("POST", "/user/change-password"): {"fields": ["oldPassword", "newPassword", "confirmPassword", "_csrf"]},
    ("POST", "/boss/register"): {"fields": ["name", "industry", "scale", "address", "description", "contactName", "contactPhone", "contactEmail", "longitude", "latitude", "_csrf"]},
    ("POST", "/boss/company/edit"): {"fields": ["industry", "scale", "address", "description", "contactName", "contactPhone", "contactEmail", "longitude", "latitude", "_csrf"]},
    ("POST", "/boss/job/save"): {"fields": ["id", "title", "salaryMin", "salaryMax", "city", "address", "education", "experience", "description", "requirements", "tags", "_csrf"]},
    ("POST", "/boss/job/delete/{encodedId}"): {"fields": ["_csrf"]},
    ("POST", "/resume/save"): {"fields": ["realName", "gender", "phone", "email", "birthDate", "education", "school", "major", "graduationYear", "workExperience", "skills", "selfIntroduction", "_csrf"]},
}

JSON_BODY_OVERRIDES: dict[tuple[str, str], dict[str, Any]] = {
    ("POST", "/api/chat/mark-read"): {"conversationId": "{{conversationId}}"},
    ("POST", "/api/chat/send"): {"conversationId": "{{conversationId}}", "receiverId": "{{receiverId}}", "content": "你好，这是 API 测试消息", "messageType": "text", "fileName": None, "filePath": None, "fileSize": None},
    ("POST", "/api/seeker/resume"): {"realName": "接口测试用户", "gender": "男", "phone": "13900139999", "email": "api_test@example.com", "birthday": "2000-01-01", "education": "本科", "school": "测试大学", "major": "计算机科学", "workExperience": "1 年 Java 开发经验", "projectExperience": "微招系统接口测试", "skills": "Java,Spring Boot,MySQL", "selfIntroduction": "用于接口测试的简历数据"},
    ("POST", "/boss/application/{encodedId}/interview"): {"interviewTime": "2026-07-20T10:00:00", "interviewLocation": "北京市海淀区中关村", "bossNote": "请携带纸质简历"},
    ("POST", "/api/mobile/notifications/push-token"): {"installationId": "android-emulator-api-test", "pushToken": "test-push-token-not-for-production"},
    ("POST", "/api/mobile/resume-access/requests"): {"seekerId": "{{encodedSeekerId}}"},
    ("POST", "/api/mobile/resume-access/requests/{id}/decision"): {"approved": True},
    ("PUT", "/api/admin/admins/{userId}"): {"roleType": "operator"},
    ("PUT", "/api/admin/companies/{id}/reject"): {"reason": "公司资质信息不完整"},
    ("POST", "/api/admin/complaints/{id}/reject"): {"reason": "证据不足，驳回投诉"},
    ("PUT", "/api/admin/identities/users/{userId}/{role}/disable"): {"reason": "接口测试：临时停用身份"},
    ("PUT", "/api/admin/identities/users/{userId}/{role}/enable"): {"reason": "接口测试：恢复身份"},
    ("POST", "/api/admin/jobs/batch-offline"): {"ids": [1, 2, 3]},
    ("PUT", "/api/admin/jobs/{id}/reject"): {"reason": "职位描述不完整"},
    ("POST", "/api/admin/sanctions/{id}/revoke"): {"reason": "申诉通过，撤销处罚"},
    ("POST", "/api/admin/sessions/users/{userId}/force-logout"): {"reason": "接口测试：强制失效全部会话"},
    ("PUT", "/api/admin/users/{id}/reset-password"): {"newPassword": "Weib@654321"},
}

MULTIPART_BODY_OVERRIDES: dict[tuple[str, str], dict[str, Any]] = {
    ("POST", "/api/appeals/evidence"): {"file": "@C:/test-data/appeal-evidence.png"},
    ("POST", "/api/forum/media"): {"file": "@C:/test-data/forum-image.png"},
    ("POST", "/api/seeker/resume/media"): {"file": "@C:/test-data/resume.pdf"},
    ("POST", "/chat/upload"): {"file": "@C:/test-data/chat-image.png"},
}


def platform_for(path: str, page_route: bool) -> str:
    if path.startswith("/api/test/"):
        return "测试工具（默认关闭）"
    if path.startswith("/api/mobile/"):
        return APP_BADGE
    if path.startswith("/api/admin/") or path.startswith("/admin"):
        return "管理后台"
    if page_route:
        return "页面路由"
    if path.startswith(("/api/forum/", "/api/complaints", "/api/appeals", "/api/chat/", "/captcha")):
        return "Web/App 通用"
    return "Web 专用"


def module_for(path: str, page_route: bool) -> str:
    if page_route:
        return "pages"
    if path.startswith("/api/mobile/"):
        return "mobile"
    if path.startswith("/api/admin/") or path.startswith("/admin"):
        return "admin"
    if any(word in path for word in ("login", "logout", "register", "captcha", "username", "password", "identities")):
        return "auth"
    if path.startswith(("/boss", "/api/boss")):
        return "boss"
    if path.startswith(("/api/forum", "/forum", "/api/complaints", "/api/appeals", "/appeal")):
        return "community"
    if any(word in path for word in ("media", "upload", "/file/", "/export", "/download")):
        return "files"
    return "seeker"


RESOURCE_LABELS = {
    "auth": "认证", "login": "登录", "logout": "退出登录", "me": "当前用户", "captcha": "验证码",
    "jobs": "职位", "job": "职位", "applications": "投递", "application": "投递", "favorites": "收藏",
    "favorite": "收藏", "resume": "简历", "resumes": "简历", "company": "公司", "companies": "公司",
    "dashboard": "工作台", "talents": "人才", "notifications": "通知", "notification": "通知",
    "forum": "论坛", "posts": "帖子", "post": "帖子", "comments": "评论", "comment": "评论",
    "sections": "论坛板块", "media": "媒体文件", "complaints": "投诉", "appeals": "申诉",
    "users": "用户", "identities": "身份", "sessions": "会话", "audit-logs": "审计日志",
    "search": "搜索", "stats": "统计", "messages": "消息", "conversations": "会话",
    "read": "已读状态", "push-token": "推送令牌", "requests": "访问申请", "map": "地图",
}
ACTION_LABELS = {
    "GET": "查询", "POST": "提交", "PUT": "更新", "PATCH": "更新", "DELETE": "删除",
    "approve": "审核通过", "reject": "审核拒绝", "ban": "封禁", "unban": "解除封禁",
    "disable": "停用", "enable": "启用", "close": "关闭", "reopen": "重新开放",
    "withdraw": "撤回", "apply": "投递", "favorite": "收藏/取消收藏", "read": "标记已读",
    "decision": "处理决定", "interview": "安排面试", "force-logout": "强制退出", "reset-password": "重置密码",
}


def function_for(endpoint: Endpoint, operation: dict[str, Any] | None) -> str:
    if endpoint.path == "/api/test/captcha":
        return "获取 API 自动化测试验证码明文"
    tokens = [token for token in endpoint.path.split("/") if token and not token.startswith("{")]
    last = tokens[-1] if tokens else "home"
    action = ACTION_LABELS.get(last, ACTION_LABELS.get(endpoint.method, "访问"))
    resource = ""
    for token in reversed(tokens):
        if token in RESOURCE_LABELS:
            resource = RESOURCE_LABELS[token]
            break
    if last in ("login", "logout"):
        return RESOURCE_LABELS[last]
    if last == "captcha":
        return "获取登录/注册验证码图片"
    if endpoint.page_route:
        return f"打开{resource or '系统'}页面"
    if operation and operation.get("summary"):
        return str(operation["summary"])
    return f"{action}{resource or '业务数据'}"


def _resolve_schema(schema: dict[str, Any], spec: dict[str, Any]) -> dict[str, Any]:
    seen: set[str] = set()
    while "$ref" in schema:
        ref = schema["$ref"]
        if ref in seen:
            break
        seen.add(ref)
        node: Any = spec
        for part in ref.lstrip("#/").split("/"):
            node = node.get(part, {}) if isinstance(node, dict) else {}
        schema = node if isinstance(node, dict) else {}
    return schema


def _sample(name: str, schema: dict[str, Any], spec: dict[str, Any]) -> Any:
    schema = _resolve_schema(schema or {}, spec)
    key = re.sub(r"[^a-z0-9]", "", name.lower())
    if key in SAMPLE_VALUES:
        return SAMPLE_VALUES[key]
    if "example" in schema:
        return schema["example"]
    if schema.get("enum"):
        return schema["enum"][0]
    kind = schema.get("type")
    if kind == "object" or "properties" in schema:
        return {k: _sample(k, v, spec) for k, v in schema.get("properties", {}).items() if not k.lower().endswith(("createdat", "updatedat"))}
    if kind == "array":
        return [_sample(name.rstrip("s") or "item", schema.get("items", {}), spec)]
    if kind in ("integer", "number"):
        return 1
    if kind == "boolean":
        return True
    if schema.get("format") == "date":
        return "2026-07-14"
    if schema.get("format") == "date-time":
        return "2026-07-14T10:00:00+08:00"
    return SAMPLE_VALUES.get(key, f"api-test-{name}")


def _schema_type(schema: dict[str, Any], spec: dict[str, Any]) -> str:
    schema = _resolve_schema(schema or {}, spec)
    if schema.get("type"):
        return str(schema["type"])
    return "object" if "properties" in schema else "string"


def _value_text(value: Any) -> str:
    if isinstance(value, (dict, list)):
        return json.dumps(value, ensure_ascii=False)
    if isinstance(value, bool):
        return "true" if value else "false"
    return str(value)


def _request_body(operation: dict[str, Any], spec: dict[str, Any]) -> tuple[str, dict[str, Any], list[ParameterRow]]:
    content = operation.get("requestBody", {}).get("content", {})
    if not content:
        return "无", {}, []
    media = next((m for m in ("application/json", "multipart/form-data", "application/x-www-form-urlencoded") if m in content), next(iter(content)))
    schema = _resolve_schema(content[media].get("schema", {}), spec)
    sample = _sample("body", schema, spec)
    if not isinstance(sample, dict):
        sample = {"value": sample}
    required_fields = set(schema.get("required", []))
    rows = []
    for name, field_schema in schema.get("properties", {}).items():
        rows.append(ParameterRow(name, "body", "是" if name in required_fields else "否", _schema_type(field_schema, spec), _value_text(sample.get(name, _sample(name, field_schema, spec))), str(field_schema.get("description", "请求体字段"))))
    return media, sample, rows


def _permission(path: str, page_route: bool) -> str:
    if path == "/api/test/captcha":
        return "测试工具；默认关闭；仅在 TEST_CAPTCHA_API_ENABLED=true 且 X-Test-Access-Key 正确时开放"
    if path in ("/login", "/register", "/captcha", "/check-username", "/api/admin/auth/login", "/api/mobile/auth/login"):
        return "公开；必须在同一 Cookie 会话中获取验证码并完成登录/注册"
    if path.startswith("/api/admin/"):
        return "管理员；请求头 Authorization: Bearer {{adminToken}}"
    if path.startswith("/api/mobile/"):
        if path.startswith("/api/mobile/auth/") and path.endswith("login"):
            return "公开；先在同一 Cookie 会话获取验证码"
        return "已登录 App 用户；Authorization: Bearer {{mobileToken}}；角色按接口要求为 SEEKER 或 BOSS"
    if page_route and path in ("/", "/login", "/register", "/forum"):
        return "公开页面"
    if path.startswith("/api/forum/") and endpoint_public_forum(path):
        return "公开读取；写操作必须登录"
    return "Web 用户已登录；保持 JSESSIONID Cookie，写操作同时携带 _csrf 或 X-CSRF-TOKEN"


def endpoint_public_forum(path: str) -> bool:
    return path in ("/api/forum/sections", "/api/forum/posts") or bool(re.fullmatch(r"/api/forum/posts/\{[^}]+\}", path))


def _headers(endpoint: Endpoint, content_type: str, operation: dict[str, Any] | None) -> tuple[tuple[str, str], ...]:
    values: list[tuple[str, str]] = []
    if endpoint.path == "/api/test/captcha":
        values.append(("X-Test-Access-Key", "{{testCaptchaAccessKey}}"))
    elif endpoint.path.startswith("/api/admin/") and endpoint.path != "/api/admin/auth/login":
        values.append(("Authorization", "Bearer {{adminToken}}"))
    elif endpoint.path.startswith("/api/mobile/") and endpoint.path != "/api/mobile/auth/login":
        values.append(("Authorization", "Bearer {{mobileToken}}"))
    elif not endpoint.page_route and endpoint.path not in ("/check-username", "/captcha", "/api/test/captcha"):
        values.append(("Cookie", "JSESSIONID={{jsessionid}}"))
    if content_type != "无":
        values.append(("Content-Type", content_type))
    if endpoint.method in ("POST", "PUT", "PATCH", "DELETE"):
        if endpoint.path.startswith("/api/") and endpoint.path not in ("/api/admin/auth/login", "/api/mobile/auth/login"):
            values.append(("X-CSRF-TOKEN", "{{csrfToken}}（采用 Session 认证时）"))
        if operation and any(p.get("name") == "Idempotency-Key" for p in operation.get("parameters", [])):
            values.append(("Idempotency-Key", "{{idempotencyKey}}"))
    return tuple(dict(values).items())


def _parameter_rows(endpoint: Endpoint, operation: dict[str, Any] | None, spec: dict[str, Any]) -> tuple[list[ParameterRow], dict[str, Any], str]:
    rows: list[ParameterRow] = []
    query: dict[str, Any] = {}
    if operation:
        for p in operation.get("parameters", []):
            schema = p.get("schema", {})
            value = _sample(str(p.get("name", "value")), schema, spec)
            location = str(p.get("in", "query"))
            rows.append(ParameterRow(str(p.get("name")), location, "是" if p.get("required") else "否", _schema_type(schema, spec), _value_text(value), str(p.get("description", ""))))
            if location == "query":
                query[str(p.get("name"))] = value
        content_type, body, body_rows = _request_body(operation, spec)
        key = (endpoint.method, endpoint.path)
        if key in JSON_BODY_OVERRIDES:
            body = JSON_BODY_OVERRIDES[key]
            body_rows = [ParameterRow(name, "body", "是", "array" if isinstance(value, list) else "boolean" if isinstance(value, bool) else "string", _value_text(value), "JSON 请求体字段") for name, value in body.items()]
            content_type = "application/json"
        if key in MULTIPART_BODY_OVERRIDES:
            body = MULTIPART_BODY_OVERRIDES[key]
            body_rows = [ParameterRow(name, "form-data", "是", "binary", _value_text(value), "本地上传文件；单文件不超过 10MB") for name, value in body.items()]
            content_type = "multipart/form-data"
        rows.extend(body_rows)
        return rows, body, content_type
    override = FORM_OVERRIDES.get((endpoint.method, endpoint.path))
    if override:
        body = {name: SAMPLE_VALUES.get(re.sub(r"[^a-z0-9]", "", name.lower()), "{{csrfToken}}" if name == "_csrf" else f"api-test-{name}") for name in override["fields"]}
        for name, value in body.items():
            rows.append(ParameterRow(name, "form", "否" if name in ("id", "industry", "scale", "address", "description", "requirements", "tags", "longitude", "latitude") else "是", "string", _value_text(value), "表单字段"))
        return rows, body, "application/x-www-form-urlencoded"
    for name in re.findall(r"\{([^}]+)\}", endpoint.path):
        value = _sample(name, {}, spec)
        rows.append(ParameterRow(name, "path", "是", "string", _value_text(value), "路径变量"))
    return rows, {}, "无"


def _sample_url(endpoint: Endpoint, rows: Iterable[ParameterRow]) -> str:
    path = endpoint.path
    query: dict[str, str] = {}
    for row in rows:
        if row.location == "path":
            path = path.replace("{" + row.name + "}", row.example)
        elif row.location == "query":
            query[row.name] = row.example
    return BASE_URL + path + (("?" + urlencode(query)) if query else "")


def _request_sample(endpoint: Endpoint, url: str, content_type: str, headers: tuple[tuple[str, str], ...], body: dict[str, Any]) -> str:
    parts = [f"{endpoint.method} {url}"]
    parts.extend(f"{name}: {value}" for name, value in headers)
    if endpoint.page_route:
        parts.append("响应类型: text/html；在浏览器中直接访问该 URL")
    elif content_type == "application/json":
        parts.append("")
        parts.append(json.dumps(body, ensure_ascii=False, indent=2) if body else "请求体：无")
    elif content_type == "multipart/form-data":
        parts.append("")
        if body:
            parts.extend(f"{name}={value}" for name, value in body.items())
        else:
            parts.append("file=@C:/test-data/test-image.png")
    elif content_type == "application/x-www-form-urlencoded":
        parts.append("")
        parts.append(urlencode({k: _value_text(v) for k, v in body.items()}))
    else:
        parts.append("")
        parts.append("请求体：无")
    return "\n".join(parts)


def _response_sample(endpoint: Endpoint, operation: dict[str, Any] | None, spec: dict[str, Any]) -> str:
    if endpoint.page_route or endpoint.path in ("/login", "/logout", "/register") and endpoint.method == "POST":
        return "HTTP/1.1 302 Found\nLocation: /目标页面\n（页面接口成功时返回 HTML 或重定向）"
    if endpoint.path == "/captcha":
        return "HTTP/1.1 200 OK\nContent-Type: image/png\nX-Captcha-Expires-In: 120\n\n<PNG 二进制图片>"
    if endpoint.path == "/api/test/captcha":
        return json.dumps({
            "code": 200,
            "msg": "操作成功",
            "data": {"captcha": "AB12", "expiresInSeconds": 120},
        }, ensure_ascii=False, indent=2)
    if endpoint.path == "/chat/file/{storedName}":
        return "HTTP/1.1 200 OK\nContent-Type: image/png（按实际文件）\nContent-Disposition: inline; filename=storedName\n\n<文件二进制内容>"
    if endpoint.path.startswith("/api/admin/export/"):
        return "HTTP/1.1 200 OK\nContent-Type: text/csv;charset=UTF-8\nContent-Disposition: attachment; filename=export.csv\n\n<CSV 文件内容>"
    schema: dict[str, Any] = {}
    if operation:
        responses = operation.get("responses", {})
        response = responses.get("200") or responses.get("201") or next(iter(responses.values()), {})
        content = response.get("content", {}) if isinstance(response, dict) else {}
        if content:
            media = next(iter(content.values()))
            schema = media.get("schema", {}) if isinstance(media, dict) else {}
    sample = _sample("response", schema, spec) if schema else {"code": 200, "msg": "success", "data": {}}
    if not isinstance(sample, dict) or not {"code", "msg"}.intersection(sample):
        sample = {"code": 200, "msg": "success", "data": sample}
    if "code" in sample:
        sample["code"] = 200
    if "msg" in sample:
        sample["msg"] = "success"
    return json.dumps(sample, ensure_ascii=False, indent=2)


def _errors(endpoint: Endpoint) -> tuple[str, ...]:
    if endpoint.path == "/api/test/captcha":
        return (
            "404：测试开关关闭、访问密钥缺失或访问密钥错误",
            "429：验证码请求过于频繁，请按 Retry-After 等待",
        )
    values = ["400：请求参数、格式或业务状态不符合要求"]
    permission = _permission(endpoint.path, endpoint.page_route)
    if "已登录" in permission or "管理员" in permission or "App 用户" in permission:
        values.extend(["401：未登录、会话过期或 Token 无效", "403：当前身份或权限不足"])
    if endpoint.method in ("POST", "PUT", "PATCH", "DELETE"):
        values.append("409：幂等请求正在处理、资源状态冲突或重复提交")
    if re.search(r"\{[^}]+\}", endpoint.path):
        values.append("404：路径变量对应的资源不存在")
    if "media" in endpoint.path or "upload" in endpoint.path:
        values.append("413/415：文件过大或文件类型不受支持")
    return tuple(values)


def _variable_source(endpoint: Endpoint, rows: Iterable[ParameterRow]) -> str:
    names = {row.name.lower() for row in rows}
    notes = []
    mapping = {
        "jobid": "{{jobId}} 从职位列表或职位创建响应取得",
        "encodedid": "{{encodedJobId}} 从职位列表/页面链接取得",
        "applicationid": "{{applicationId}} 从投递列表取得",
        "postid": "{{postId}} 从论坛帖子列表或发帖响应取得",
        "commentid": "{{commentId}} 从帖子详情评论列表取得",
        "userid": "{{userId}} 从管理员用户列表或当前用户信息取得",
        "conversationid": "{{conversationId}} 从求职者/Boss 会话列表取得",
        "receiverid": "{{receiverId}} 从会话参与者或投递详情取得",
        "seekerid": "{{encodedSeekerId}} 从 App 人才列表取得",
        "storedname": "{{storedName}} 从聊天文件上传成功响应取得",
    }
    for key, text in mapping.items():
        if key in names:
            notes.append(text)
    if endpoint.path == "/api/test/captcha":
        notes.append("{{testCaptchaAccessKey}} 来自测试环境变量 TEST_CAPTCHA_ACCESS_KEY；响应 data.captcha 可直接作为登录 captcha")
    elif endpoint.path.startswith("/api/mobile/"):
        notes.append("{{mobileToken}} 从 APP 登录接口 data.accessToken 取得")
    elif endpoint.path.startswith("/api/admin/") and endpoint.path != "/api/admin/auth/login":
        notes.append("{{adminToken}} 从管理员登录接口 data.token 取得")
    elif endpoint.method in ("POST", "PUT", "PATCH", "DELETE"):
        notes.append("{{jsessionid}} 与 {{csrfToken}} 来自同一 Web 登录会话")
    return "；".join(notes) if notes else "无动态变量；可直接使用示例值。"


def build_catalog(endpoints: list[Endpoint], openapi_path: Path) -> list[InterfaceCard]:
    spec = json.loads(openapi_path.read_text(encoding="utf-8"))
    module_order = {name: i for i, name in enumerate(("auth", "seeker", "boss", "community", "files", "mobile", "admin", "pages"))}
    working: list[dict[str, Any]] = []
    for endpoint in endpoints:
        operation = spec.get("paths", {}).get(endpoint.path, {}).get(endpoint.method.lower())
        rows, body, content_type = _parameter_rows(endpoint, operation, spec)
        url = _sample_url(endpoint, rows)
        headers = _headers(endpoint, content_type, operation)
        working.append({"endpoint": endpoint, "operation": operation, "module": module_for(endpoint.path, endpoint.page_route), "rows": rows, "body": body, "content_type": content_type, "url": url, "headers": headers})
    working.sort(key=lambda item: (module_order[item["module"]], item["endpoint"].path, item["endpoint"].method))
    counters: dict[str, int] = {}
    prefixes = {"auth": "AUTH", "seeker": "SEEKER", "boss": "BOSS", "community": "COMM", "files": "FILE", "mobile": "APP", "admin": "ADMIN", "pages": "PAGE"}
    cards: list[InterfaceCard] = []
    for item in working:
        module = item["module"]
        counters[module] = counters.get(module, 0) + 1
        endpoint: Endpoint = item["endpoint"]
        operation = item["operation"]
        cards.append(InterfaceCard(
            code=f"{prefixes[module]}-{counters[module]:03d}", platform=platform_for(endpoint.path, endpoint.page_route),
            module=module, method=endpoint.method, path=endpoint.path, full_url=item["url"],
            function=function_for(endpoint, operation), permission=_permission(endpoint.path, endpoint.page_route),
            content_type=item["content_type"], headers=item["headers"], parameters=tuple(item["rows"]),
            request_sample=_request_sample(endpoint, item["url"], item["content_type"], item["headers"], item["body"]),
            success_response=_response_sample(endpoint, operation, spec), variable_source=_variable_source(endpoint, item["rows"]),
            key_errors=_errors(endpoint), page_route=endpoint.page_route,
        ))
    return cards
