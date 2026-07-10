# 微招系统完整接口测试文档

> 文档版本：2.0  
> 适用环境：`http://superorange.top`  
> 编写日期：2026-07-10  
> 用途：使用 Apifox、Postman 或 cURL 练习微招系统接口，不需要对照前端页面猜测参数。

## 快速找到登录接口

- 普通用户登录：[`POST /login`](#post-login)
- 管理员登录：[`POST /api/admin/auth/login`](#post-apiadminauthlogin)

普通用户和管理员使用两套不同的认证方式：

| 类型 | 登录接口 | 认证方式 | 主要适用模块 |
|---|---|---|---|
| 普通用户、求职者、Boss | `POST /login` | `JSESSIONID` Session；服务器还会设置用户 JWT Cookie | 求职者、Boss、聊天、通知 |
| 管理员 | `POST /api/admin/auth/login` | `Authorization: Bearer <adminToken>` | `/api/admin/**` |

---

## 目录

1. [测试准备](#1-测试准备)
2. [通用约定](#2-通用约定)
3. [普通用户登录完整练习](#3-普通用户登录完整练习)
4. [管理员登录完整练习](#4-管理员登录完整练习)
5. [账号与公共接口](#5-账号与公共接口)
6. [求职者接口](#6-求职者接口)
7. [Boss、公司与职位管理接口](#7-boss公司与职位管理接口)
8. [聊天、文件与通知接口](#8-聊天文件与通知接口)
9. [管理后台接口](#9-管理后台接口)
10. [地图与公共数据接口](#10-地图与公共数据接口)
11. [错误、限流与幂等](#11-错误限流与幂等)
12. [练习用例与变量记录表](#12-练习用例与变量记录表)
13. [页面型路由附录](#13-页面型路由附录)

---

## 1. 测试准备

### 1.1 基础地址

```text
baseUrl=http://superorange.top
```

除非某个接口单独说明，本文中的完整地址都按 `{{baseUrl}} + 接口路径` 拼接。例如：

```text
POST {{baseUrl}}/api/admin/auth/login
POST http://superorange.top/api/admin/auth/login
```

### 1.2 推荐环境变量

| 变量 | 示例 | 获取方式 |
|---|---|---|
| `baseUrl` | `http://superorange.top` | 固定测试环境 |
| `JSESSIONID` | `A1B2...` | `GET /login` 的 `Set-Cookie` |
| `csrfToken` | `Z8Pp...` | `GET /login` HTML 中隐藏字段 `_csrf` |
| `captcha` | 人工识别的4位字符 | `GET /captcha` PNG 图片 |
| `adminToken` | `eyJhbGci...` | 管理员登录响应 `data.token` |
| `encodedJobId` | `...` | 职位列表/详情响应或页面链接 |
| `encodedApplicationId` | `...` | 投递列表响应 |
| `encodedCompanyId` | `...` | 公司/职位响应 |
| `conversationId` | `app_123` | 会话列表响应 |
| `receiverId` | `12` | 会话参与者信息 |
| `idempotencyKey` | UUID | 新操作新建，重试同一操作时复用 |
| `clientMessageId` | UUID | 客户端生成的聊天消息唯一标识 |

### 1.3 测试工具设置

1. 启用 Cookie Jar/自动保存 Cookie。
2. 普通用户的 `GET /login`、`GET /captcha`、`POST /login` 必须使用同一个 Cookie 会话。
3. 普通用户表单接口通常使用 `application/x-www-form-urlencoded`。
4. `/api/**` JSON 接口通常使用 `application/json`，但以每个接口的说明为准。
5. 为了观察普通用户登录的 `302`，建议临时关闭“自动跟随重定向”。
6. 不要把管理员真实密码、服务器密码或 Token 保存到公共集合。

---

## 2. 通用约定

### 2.1 JSON 响应结构

多数 JSON 接口返回：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | integer | 业务状态码 |
| `msg` | string | 业务消息 |
| `data` | object/array/null | 业务数据 |

注意：部分 Controller 在认证失败时仍可能返回 HTTP 200，但响应体 `code=401`。练习时同时检查 HTTP 状态码和业务 `code`。

### 2.2 HTML 表单响应

`POST /login`、`POST /register`、`POST /resume/save` 等接口来自传统 MVC 表单：

- 成功时可能返回 `302 Location: ...`。
- 失败时可能返回 HTTP 200 的 HTML 页面，错误文字嵌在页面中。
- 不能只以 HTTP 200 判断业务成功。

### 2.3 参数规则

| 字段 | 规则 | 合法示例 | 非法/边界练习 |
|---|---|---|---|
| `username`（新建） | 3–32位；字母、数字、下划线、中文 | `tester_01` | 2位、33位、`bad-name` |
| `username`（登录） | 用户名或手机号；非空；最多32位 | `tester_01` | 空、33位 |
| `password`（新建/修改/重置） | 8–64位；同时包含大写、小写、数字；不能与用户名/手机号相同 | `TestUser123` | 7位、65位、缺任一字符种类 |
| `password`（登录） | 非空；最多64位 | 当前账号密码 | 空、65位 |
| `phone` | `^1[3-9]\d{9}$` | `13800138000` | `12800138000`、10位、12位 |
| `captcha` | 图片中的4位字符；120秒有效 | 人工读取 | 空、错误、过期、旧验证码 |
| `Idempotency-Key` | 8–128位；`A-Z a-z 0-9 . _ : -` | UUID | 少于8位、含空格、含`!` |
| `clientMessageId` | 8–64位；同一发送者内唯一 | UUID | 重复值、含空格 |
| 日期时间 | ISO-8601 本地日期时间 | `2026-07-15T14:30:00` | `2026/07/15 14:30` |

### 2.4 `encodedId` 说明

带有 `encodedId`、`encodedUserId` 的路径参数是系统混淆后的字符串，不能随意把数据库数字 ID 填进去。应从职位列表、投递列表、公司详情、会话列表或相应页面链接中取得。例如：

```text
/api/seeker/job/{{encodedJobId}}
/application/{{encodedApplicationId}}/withdraw
```

### 2.5 CSRF 规则

受 CSRF 保护的普通用户写请求必须携带以下一种形式，并复用保存该 Token 的同一 Session：

```text
表单字段：_csrf={{csrfToken}}
请求头：X-CSRF-Token: {{csrfToken}}
```

`/api/admin/**` 使用管理员 JWT，不使用这套 Session CSRF。部分 `/api/seeker/**` 和 `/api/chat/**` 已从 CSRF 拦截器排除，但仍需要有效普通用户 Session；各接口会单独标明。

### 2.6 幂等规则

标记为幂等的写接口需要：

```http
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

传统表单也可以使用：

```text
_idempotencyKey=550e8400-e29b-41d4-a716-446655440000
```

- 首次操作：生成新键。
- 网络重试同一操作：复用原键。
- 新的业务操作：使用新键。
- 相同键正在处理：通常 HTTP 409。
- 相同键已完成：可能返回 `duplicate=true`，不重复执行业务。

---

## 3. 普通用户登录完整练习

### 3.1 调用顺序

```text
GET /login → 保存 JSESSIONID 和提取 _csrf
      ↓
GET /captcha → 同一 JSESSIONID 下查看验证码图片
      ↓
POST /login → 提交账号、密码、验证码、_csrf
      ↓
调用需要登录的接口验证 Session
```

#### 第一步：GET /login

请求：

```http
GET http://superorange.top/login
```

需要保存响应头中的 Cookie：

```http
Set-Cookie: JSESSIONID=<session-id>; Path=/; HttpOnly; SameSite=Lax
```

并从 HTML 中提取：

```html
<input type="hidden" name="_csrf" value="这里是csrfToken">
```

#### 第二步：GET /captcha

下面的接口必须复用第一步的 `JSESSIONID`。

### `GET /captcha`

**用途：** 获取普通用户登录/注册所需的 PNG 图片验证码。  
**完整URL：** `http://superorange.top/captcha`  
**认证：** 不要求登录，但要求保存并复用 Session Cookie。  
**Content-Type：** 无请求体；响应为 `image/png`。

请求头：

| 参数 | 必填 | 示例 | 说明 |
|---|---:|---|---|
| `Cookie` | 是 | `JSESSIONID={{JSESSIONID}}` | 与 `GET /login` 同一会话 |

响应头示例：

```http
HTTP/1.1 200
Content-Type: image/png;charset=UTF-8
X-Captcha-Expires-In: 120
Cache-Control: no-store, must-revalidate
```

测试点：

- 正常请求返回 PNG，记录 `X-Captcha-Expires-In=120`。
- 立即不停刷新可能返回 HTTP 429。
- 同一会话5秒内重复刷新会触发冷却限制。
- 验证码过期、校验成功或失败次数过多后需要获取新验证码。

#### 第三步：POST /login

<a id="post-login"></a>

### `POST /login`

**用途：** 普通用户、求职者和 Boss 登录。  
**完整URL：** `http://superorange.top/login`  
**认证：** 登录前接口；必须复用验证码所在的 `JSESSIONID` 并提交 CSRF。  
**Content-Type：** `application/x-www-form-urlencoded`。  
**限流：** 同一 IP 60秒最多10次。

表单参数：

| 参数 | 类型 | 必填 | 规则 | 示例 |
|---|---|---:|---|---|
| `username` | string | 是 | 用户名或手机号；最多32位 | `tester_01` |
| `password` | string | 是 | 当前密码；最多64位 | `TestUser123` |
| `captcha` | string | 是 | 当前 Session 图片中的4位字符 | `A7K2` |
| `_csrf` | string | 是 | 第一步提取的 Token | `{{csrfToken}}` |

cURL 完整练习：

```bash
# 1. 获取登录页、保存 Cookie
curl -c cookie.txt http://superorange.top/login -o login.html

# 2. 从 login.html 的 name="_csrf" 隐藏字段读取 csrfToken

# 3. 获取验证码，打开 captcha.png 人工识别
curl -b cookie.txt http://superorange.top/captcha -o captcha.png

# 4. 关闭自动跟随重定向并提交登录
curl -i -b cookie.txt -c cookie.txt \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "username=tester_01" \
  --data-urlencode "password=TestUser123" \
  --data-urlencode "captcha=A7K2" \
  --data-urlencode "_csrf={{csrfToken}}" \
  http://superorange.top/login
```

成功特征：

```http
HTTP/1.1 302
Location: /
Set-Cookie: JSESSIONID=<新的session-id>; ...
Set-Cookie: remember_token=...; HttpOnly; ...
Set-Cookie: jwt_token=...; HttpOnly; ...
```

常见失败：

| 表现 | 可能原因 | 排查 |
|---|---|---|
| 200并返回登录页HTML | 验证码错误、账号密码错误、参数格式错误 | 搜索响应HTML中的错误文字 |
| 302跳回`/login` | 缺少/错误CSRF或Session不一致 | 检查Cookie Jar和`_csrf` |
| 提示验证码错误 | 验证码过期、用错Session、旧验证码 | 同一Cookie重新获取验证码 |
| 提示锁定15分钟 | 连续密码错误达到锁定阈值 | 等待锁定结束后再测 |
| 429 | 登录请求过于频繁 | 等待限流窗口恢复 |

练习点：正确登录、错误验证码、过期验证码、错误密码、空账号、超长账号、缺少CSRF、验证码与登录使用不同Cookie。

#### 第四步：验证 Session

登录后用同一个 Cookie Jar 请求：

```http
GET http://superorange.top/api/seeker/applications
Cookie: JSESSIONID={{JSESSIONID}}
```

若账号是求职者且 Session 有效，应返回 JSON 业务结果，而不是“请先以求职者身份登录”。

---

## 4. 管理员登录完整练习

<a id="post-apiadminauthlogin"></a>

### `POST /api/admin/auth/login`

**用途：** 管理后台登录并取得 JWT。  
**完整URL：** `http://superorange.top/api/admin/auth/login`  
**认证：** 无。  
**Content-Type：** `application/json`。

JSON Body：

| 参数 | 类型 | 必填 | 规则 | 示例 |
|---|---|---:|---|---|
| `username` | string | 是 | 管理员用户名；最多32位 | `admin_practice` |
| `password` | string | 是 | 当前密码；最多64位 | `AdminTest123` |

请求示例：

```http
POST /api/admin/auth/login HTTP/1.1
Host: superorange.top
Content-Type: application/json

{
  "username": "admin_practice",
  "password": "AdminTest123"
}
```

成功响应结构：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "admin": {
      "id": 1,
      "username": "admin_practice",
      "nickname": "练习管理员",
      "roleType": "super_admin"
    }
  }
}
```

保存 `data.token` 为 `adminToken`。后续管理员接口添加：

```http
Authorization: Bearer {{adminToken}}
```

失败响应可能是 HTTP 200、响应体业务码401：

```json
{
  "code": 401,
  "msg": "用户名或密码错误",
  "data": null
}
```

测试点：正确管理员、错误密码、普通用户账号尝试后台登录、被封禁管理员、连续失败锁定、空Token、损坏Token。

### `GET /api/admin/auth/me`

**用途：** 验证管理员 Token 并取得当前管理员信息。  
**完整URL：** `http://superorange.top/api/admin/auth/me`  
**认证：** 管理员 JWT。  
**请求体：** 无。

请求示例：

```http
GET /api/admin/auth/me HTTP/1.1
Host: superorange.top
Authorization: Bearer {{adminToken}}
```

成功时 `data` 包含 `id`、`username`、`nickname`、`roleType`。无效或过期 Token 返回401。

### `POST /api/admin/auth/logout`

**用途：** 管理后台退出。服务端接口返回成功；客户端还必须删除本地 `adminToken`。  
**完整URL：** `http://superorange.top/api/admin/auth/logout`  
**认证：** 管理员 JWT。  
**请求体：** 无。

```http
POST /api/admin/auth/logout HTTP/1.1
Host: superorange.top
Authorization: Bearer {{adminToken}}
```

练习时退出后清空环境变量，再调用 `/api/admin/auth/me`，应得到401。

---

## 5. 账号与公共接口

> 本节将在后续接口清单中展开。

## 6. 求职者接口

> 本节将在后续接口清单中展开。

## 7. Boss、公司与职位管理接口

> 本节将在后续接口清单中展开。

## 8. 聊天、文件与通知接口

> 本节将在后续接口清单中展开。

## 9. 管理后台接口

> 本节将在后续接口清单中展开。

## 10. 地图与公共数据接口

> 本节将在后续接口清单中展开。

## 11. 错误、限流与幂等

> 本节将在后续测试清单中展开。

## 12. 练习用例与变量记录表

> 本节将在后续测试清单中展开。

## 13. 页面型路由附录

> 本节仅列出返回 HTML 的页面路由，后续补全。

