# API 测试规则与安全约束

> Swagger UI：`https://服务器地址:8443/swagger-ui/index.html`  
> OpenAPI JSON：`https://服务器地址:8443/v3/api-docs`

## 1. 通用输入规则

| 字段 | 新建/修改规则 | 登录规则 | 合法示例 | 非法示例 |
|---|---|---|---|---|
| `username` | 3–32 位；字母、数字、下划线、中文 | 非空，最多 32 位；也可输入 11 位手机号 | `tester_01`、`测试用户` | `ab`、`bad-name`、33 位字符串 |
| `password` | 8–64 位；必须同时包含大写、小写、数字；不能等于用户名或手机号 | 非空，最多 64 位 | `TestUser123` | `12345678`、`abcdefgh1`、`ABCDEFGH1`、65 位字符串 |
| `phone` | `^1[3-9]\d{9}$`，固定 11 位 | 可作为登录账号 | `13800138000` | `12800138000`、`1380013800` |
| `captcha` | 4 位；Redis 有效期 120 秒；同一会话 5 秒内不可刷新 | 必填 | 图片显示值 | 空、过期值、旧验证码 |
| `role` | 普通注册只接受 `seeker`、`boss` | — | `seeker` | `admin` |
| `roleType` | `super_admin`、`auditor`、`viewer` | — | `auditor` | `root` |
| `Idempotency-Key` | 8–128 位：`A-Z a-z 0-9 . _ : -`；建议 UUID | — | `550e8400-e29b-41d4-a716-446655440000` | 空、`bad key!` |
| `clientMessageId` | 8–64 位，规则同幂等键；同一发送者内唯一 | — | UUID | 重复 ID、含空格 |

### 兼容说明

新规则用于新注册、修改密码、管理员创建和密码重置。历史测试账号仍可使用原密码登录；登录接口不会强制历史密码满足新强度规则。

## 2. 认证方式

### 用户端 HTML/Session 接口

1. GET 页面并保存 `JSESSIONID` Cookie。
2. 从页面隐藏字段读取 `_csrf`。
3. POST 表单携带 Cookie、`_csrf` 和 `_idempotencyKey`。

### 管理端 API

1. `POST /api/admin/auth/login` 获取 JWT。
2. 后续请求添加：`Authorization: Bearer <token>`。
3. 写请求同时添加：`Idempotency-Key: <UUID>`。

## 3. 账号相关接口

### `POST /register`

Content-Type：`application/x-www-form-urlencoded`

| 参数 | 必填 | 规则 |
|---|---|---|
| `username` | 是 | 3–32 位用户名规则 |
| `password` | 是 | 8–64 位强密码规则 |
| `confirmPassword` | 是 | 必须与 `password` 相同 |
| `phone` | 是 | 11 位手机号 |
| `captcha` | 是 | 当前 Session 的有效验证码 |
| `role` | 否 | `seeker` 或 `boss`，默认 `seeker` |
| `_csrf` | 是 | 页面生成的 CSRF Token |
| `_idempotencyKey` | 是 | UUID；重复提交必须复用 |

### `POST /login`

| 参数 | 必填 | 规则 |
|---|---|---|
| `username` | 是 | 用户名或手机号，最多 32 位 |
| `password` | 是 | 最多 64 位 |
| `captcha` | 是 | 当前验证码 |
| `_csrf` | 是 | CSRF Token |

### `POST /user/change-password`

| 参数 | 必填 | 规则 |
|---|---|---|
| `oldPassword` | 是 | 最多 64 位 |
| `newPassword` | 是 | 8–64 位强密码规则；不能与旧密码、用户名、手机号相同 |
| `confirmPassword` | 是 | 必须与新密码一致 |
| `_csrf` | 是 | CSRF Token |
| `_idempotencyKey` | 是 | UUID |

### `POST /api/admin/auth/login`

```json
{
  "username": "admin",
  "password": "Admin123456"
}
```

- `username`：最多 32 位。
- `password`：最多 64 位。

### `POST /api/admin/admins`

Headers：`Authorization`、`Idempotency-Key`

```json
{
  "username": "audit_user",
  "password": "TestAdmin123",
  "roleType": "auditor"
}
```

## 4. 需要幂等键的写接口

以下接口必须携带 `Idempotency-Key` 请求头；HTML 表单也可使用 `_idempotencyKey` 参数：

| 模块 | 接口 |
|---|---|
| 注册 | `POST /register` |
| 投递 | `POST /job/{encodedId}/apply` |
| 收藏 | `POST /job/{encodedId}/favorite` |
| 撤回投递 | `POST /application/{encodedId}/withdraw` |
| Boss 入驻 | `POST /boss/register` |
| 职位 | `POST /boss/job/save`、删除、重新开放 |
| 候选人 | 状态更新、面试安排 |
| 聊天 | `POST /api/chat/send` |
| 通知 | 全部已读、单条已读 |
| 管理员 | 创建、角色变更、禁用 |
| 后台审核 | 公司/职位通过、驳回、批量下线 |
| 用户管理 | 封禁、解封、重置密码 |

### 幂等响应

| HTTP | 含义 | 预期处理 |
|---:|---|---|
| `200` | 首次成功，或同键操作之前已完成 | 检查响应中的 `duplicate` |
| `400` | 幂等键缺失/格式非法，或参数校验失败 | 修正请求后生成新键 |
| `409` | 相同幂等键正在处理中 | 不要生成新键；稍后复用原键重试 |

完成态重复响应示例：

```json
{"code":200,"message":"操作已完成","duplicate":true}
```

处理中重复响应示例：

```json
{"code":409,"message":"操作处理中，请勿重复提交","duplicate":false}
```

## 5. 聊天发送接口

`POST /api/chat/send`

Headers：`Idempotency-Key`、Session Cookie、CSRF Token

```json
{
  "conversationId": "app_123",
  "receiverId": 2,
  "content": "你好",
  "messageType": "text",
  "clientMessageId": "550e8400-e29b-41d4-a716-446655440000"
}
```

相同发送者重复使用同一 `clientMessageId` 时，数据库只保留一条消息。

## 6. 建议测试用例

### 用户名边界

| 用例 | 输入长度 | 预期 |
|---|---:|---|
| 最小值以下 | 2 | 拒绝 |
| 最小值 | 3 | 接受 |
| 最大值 | 32 | 接受 |
| 最大值以上 | 33 | 拒绝 |

### 密码边界

| 用例 | 示例 | 预期 |
|---|---|---|
| 7 位 | `Aa12345` | 拒绝 |
| 8 位 | `Aa123456` | 接受 |
| 64 位且规则正确 | 大小写和数字混合 | 接受 |
| 65 位 | 大小写和数字混合 | 拒绝 |
| 无大写 | `abcdefg123` | 拒绝 |
| 无小写 | `ABCDEFG123` | 拒绝 |
| 无数字 | `AbcdefghX` | 拒绝 |
| 等于用户名/手机号 | 与身份字段相同 | 拒绝 |

### 幂等并发

1. 使用同一 `Idempotency-Key` 并发发送两次完全相同的写请求。
2. 预期只有一个请求执行；另一个返回 `409` 或完成态 `200 duplicate=true`。
3. 操作完成后再次使用同一键，预期不产生第二条业务记录。
4. 使用不同键重复测试，数据库唯一约束仍应阻止投递、收藏、公司或消息重复数据。

## 7. 常见错误码

| 代码 | 说明 |
|---:|---|
| `200` | 成功或幂等完成态重复 |
| `400` | 参数、长度、格式、幂等键错误 |
| `401` | 未登录或 JWT 无效 |
| `403` | CSRF 失败或权限不足 |
| `409` | 幂等请求正在处理 |
| `429` | 登录、注册、验证码或聊天请求过于频繁 |

## 8. 测试工具变量建议

```text
baseUrl=https://服务器地址:8443
adminToken=<登录后提取>
csrfToken=<页面隐藏字段提取>
sessionCookie=<JSESSIONID>
idempotencyKey=<每个新操作生成 UUID；重试时保持不变>
```