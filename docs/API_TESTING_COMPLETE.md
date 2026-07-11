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

### `POST /register`

**用途：** 注册求职者或 Boss 账号。

**认证：** 不要求登录；必须先用同一 `JSESSIONID` 获取注册页 CSRF 和验证码。

**Content-Type：** `application/x-www-form-urlencoded`。

**限流：** 同一 IP 60秒最多3次。

**幂等：** 必须传 `Idempotency-Key` 请求头或 `_idempotencyKey` 表单字段。

| 表单参数 | 类型 | 必填 | 规则/说明 | 示例 |
|---|---|---:|---|---|
| `username` | string | 是 | 3–32位；字母、数字、下划线、中文 | `practice_01` |
| `password` | string | 是 | 8–64位强密码 | `Practice123` |
| `confirmPassword` | string | 是 | 必须与`password`相同 | `Practice123` |
| `phone` | string | 是 | 11位大陆手机号 | `13800138000` |
| `captcha` | string | 是 | 当前Session的验证码 | 人工识别值 |
| `role` | string | 否 | `seeker`或`boss`；默认`seeker`；非法值会回落到`seeker` | `seeker` |
| `_csrf` | string | 是 | 注册页生成的CSRF Token | `{{csrfToken}}` |
| `_idempotencyKey` | string | 条件必填 | 未传同名请求头时必填 | UUID |

```http
POST /register HTTP/1.1
Host: superorange.top
Cookie: JSESSIONID={{JSESSIONID}}
Content-Type: application/x-www-form-urlencoded
Idempotency-Key: {{idempotencyKey}}

username=practice_01&password=Practice123&confirmPassword=Practice123&phone=13800138000&captcha=A7K2&role=seeker&_csrf={{csrfToken}}
```

成功时返回登录页HTML并显示“注册成功，请登录”；失败时仍可能是HTTP 200的注册页HTML。测试：重复用户名、重复手机号、密码不一致、非法角色、错误/过期验证码、缺少幂等键、同键重复注册。

### `GET /check-username`

**用途：** 检查用户名和可选手机号是否已被占用。

**认证：** 无；使用Session记录查询次数。

**限流：** 注解上限为同IP每分钟30次；同一Session内部达到5次后直接返回`rate_limited`。

| Query参数 | 类型 | 必填 | 说明 | 示例 |
|---|---|---:|---|---|
| `username` | string | 是 | 要检查的用户名 | `practice_01` |
| `phone` | string | 否 | 同时检查手机号 | `13800138000` |

```http
GET /check-username?username=practice_01&phone=13800138000 HTTP/1.1
Host: superorange.top
Cookie: JSESSIONID={{JSESSIONID}}
```

响应是纯文本，不是`Result<T>`：

| 响应 | 含义 |
|---|---|
| `available` | 用户名可用，传手机号时手机号也可用 |
| `taken` | 用户名已存在 |
| `phone_taken` | 手机号已存在 |
| `rate_limited` | 同一Session检查次数过多 |

### `POST /logout`

**用途：** 普通网页端退出，销毁Session并清除`remember_token`和`jwt_token`。

**认证：** 普通用户Session。

**CSRF：** 必须传`_csrf`或`X-CSRF-Token`。

**Content-Type：** 推荐`application/x-www-form-urlencoded`。

```http
POST /logout HTTP/1.1
Host: superorange.top
Cookie: JSESSIONID={{JSESSIONID}}
X-CSRF-Token: {{csrfToken}}
```

成功：`302 Location: /login`。退出后继续使用旧Session访问受保护接口应失败。

### `POST /user/change-password`

**用途：** 已登录用户修改密码；成功后强制退出并重新登录。

**认证：** 普通用户Session。

**CSRF：** 必须传。

**Content-Type：** `application/x-www-form-urlencoded`。

| 表单参数 | 类型 | 必填 | 规则 | 示例 |
|---|---|---:|---|---|
| `oldPassword` | string | 是 | 当前密码；最多64位 | `OldPass123` |
| `newPassword` | string | 是 | 8–64位强密码；不能与旧密码、用户名、手机号相同 | `NewPass456` |
| `confirmPassword` | string | 是 | 必须与新密码一致 | `NewPass456` |
| `_csrf` | string | 是 | 当前Session Token | `{{csrfToken}}` |

```http
POST /user/change-password HTTP/1.1
Host: superorange.top
Cookie: JSESSIONID={{JSESSIONID}}
Content-Type: application/x-www-form-urlencoded

oldPassword=OldPass123&newPassword=NewPass456&confirmPassword=NewPass456&_csrf={{csrfToken}}
```

成功：`302 Location: /login?changed`，旧Session失效。失败通常返回修改密码HTML，包含“旧密码错误”“两次输入不一致”或密码规则消息。当前源码没有给此接口添加`@Idempotent`，所以不要误加必需幂等键。

## 6. 求职者接口

### 6.1 职位与公司查询

### `GET /api/seeker/jobs`

**用途：** 分页查询、搜索和筛选在招职位。

**认证：** 可匿名；求职者登录后响应会额外正确反映`hasApplied`、`isFavorited`。

**请求体：** 无。

| Query参数 | 类型 | 必填 | 默认/规则 | 示例 |
|---|---|---:|---|---|
| `keyword` | string | 否 | 职位/公司关键词 | `Java` |
| `city` | string | 否 | 城市筛选 | `北京` |
| `education` | string | 否 | 学历筛选 | `本科` |
| `experience` | string | 否 | 经验筛选 | `1-3年` |
| `salaryMin` | integer | 否 | 最低期望薪资 | `10000` |
| `salaryMax` | integer | 否 | 最高期望薪资 | `30000` |
| `sort` | string | 否 | 默认`newest`；按服务支持值练习 | `newest` |
| `page` | integer | 否 | 从0开始；负数改为0 | `0` |
| `size` | integer | 否 | 默认12；小于1改12；最大100 | `12` |

```http
GET /api/seeker/jobs?keyword=Java&city=北京&page=0&size=12&sort=newest HTTP/1.1
Host: superorange.top
```

`data`字段：`content`、`totalElements`、`totalPages`、`number`、`size`、`first`、`last`、`empty`。从`content[].id`保存`encodedJobId`，从公司信息取得`encodedCompanyId`。

### `GET /api/seeker/job/{encodedId}`

**用途：** 查询单个活跃职位详情并增加浏览量。

**认证：** 可匿名。

**Path：** `encodedId`必填，使用职位列表返回的混淆ID。

```http
GET /api/seeker/job/{{encodedJobId}} HTTP/1.1
Host: superorange.top
Cookie: JSESSIONID={{JSESSIONID}}
```

成功返回职位、公司、投递/收藏状态。无效ID返回“参数无效”；不存在返回“职位不存在”；已关闭返回“该职位已关闭”。

### `GET /api/seeker/company/{encodedId}`

**用途：** 查询公司详情以及该公司的全部活跃职位。

**认证：** 可匿名。

**Path：** `encodedId`为公司混淆ID。

```http
GET /api/seeker/company/{{encodedCompanyId}} HTTP/1.1
Host: superorange.top
```

`data`包括`id`、`name`、`logo`、`industry`、`scale`、`address`、`description`、`contactName`、经纬度、`createdAt`和`jobs`。测试无效ID、公司不存在以及无在招职位。

### 6.2 投递与收藏

### `POST /job/{encodedId}/apply`

**用途：** 求职者投递指定职位。

**认证：** 求职者Session。

**CSRF：** 必须传。

**幂等：** 必须传`Idempotency-Key`。

**Path：** `encodedId={{encodedJobId}}`。

**请求体：** 无。

```http
POST /job/{{encodedJobId}}/apply HTTP/1.1
Host: superorange.top
Cookie: JSESSIONID={{JSESSIONID}}
X-CSRF-Token: {{csrfToken}}
Idempotency-Key: {{idempotencyKey}}
```

测试：正常投递、未登录、Boss身份、未创建简历、重复投递、下架职位、无效ID、相同幂等键重试。

### `GET /api/seeker/applications`

**用途：** 查询当前求职者全部投递。

**认证：** 求职者Session。

**请求体/参数：** 无。

响应每项包括`id`（保存为`encodedApplicationId`）、`jobId`、`status`、`bossNote`、`interviewTime`、`interviewLocation`、`rejectReason`、`createdAt`、`jobTitle`、`companyName`、`encodedCompanyId`。

### `POST /application/{encodedId}/withdraw`

**用途：** 求职者撤回自己的投递。

**认证：** 求职者Session。

**CSRF：** 必须传。

**幂等：** 必须传。

**Path：** `encodedId={{encodedApplicationId}}`。

**请求体：** 无。

```http
POST /application/{{encodedApplicationId}}/withdraw HTTP/1.1
Host: superorange.top
Cookie: JSESSIONID={{JSESSIONID}}
X-CSRF-Token: {{csrfToken}}
Idempotency-Key: {{idempotencyKey}}
```

测试他人投递ID、不可撤回状态、重复撤回和无效ID。

### `POST /job/{encodedId}/favorite`

**用途：** 切换职位收藏状态。

**认证：** 求职者Session。

**CSRF：** 必须传。

**幂等：** 必须传。

**Path：** `encodedId={{encodedJobId}}`。

**请求体：** 无。

成功`data.favorited`表示调用后的状态。注意这是“切换”接口：如果用不同幂等键重复请求会在收藏/取消之间切换；网络重试应复用同一个键。

### `GET /api/seeker/favorites`

**用途：** 查询当前求职者收藏的活跃职位。

**认证：** 求职者Session。

**参数：** 无。

返回项包括`favoriteId`、`jobId`、`title`、薪资、城市、学历、经验、标签、`companyName`、`encodedCompanyId`。下架职位不会显示。

### 6.3 简历

### `GET /api/seeker/resume`

**用途：** 查询当前求职者简历。

**认证：** 求职者Session。

**参数：** 无。

已有简历时`data.exists=true`并返回全部字段；没有简历时返回：

```json
{"code":200,"msg":"success","data":{"exists":false,"userId":12}}
```

### `POST /api/seeker/resume`

**用途：** 使用JSON新建或局部更新当前求职者简历。

**认证：** 求职者Session。

**CSRF：** `/api/seeker/**`已从CSRF拦截器排除。

**Content-Type：** `application/json`。

**幂等：** 当前源码未标注`@Idempotent`。

| JSON字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---:|---|---|
| `id` | integer | 更新时 | 数字简历ID；不传表示新建 | `3` |
| `realName` | string | 建议 | 姓名 | `张三` |
| `gender` | string | 否 | 性别 | `男` |
| `phone` | string | 建议 | 联系手机号 | `13800138000` |
| `email` | string | 建议 | 邮箱 | `tester@example.com` |
| `birthday` | string | 否 | 当前实体使用字符串 | `2000-01-01` |
| `education` | string | 否 | 学历 | `本科` |
| `school` | string | 否 | 学校 | `测试大学` |
| `major` | string | 否 | 专业 | `软件工程` |
| `workExperience` | string | 否 | 工作经历文本 | `2024-2026 ...` |
| `projectExperience` | string | 否 | 项目经历文本 | `招聘系统测试` |
| `skills` | string | 否 | 技能文本 | `Java, MySQL, Postman` |
| `selfIntroduction` | string | 否 | 自我介绍 | `两年测试经验` |

```json
{
  "realName": "张三",
  "gender": "男",
  "phone": "13800138000",
  "email": "tester@example.com",
  "birthday": "2000-01-01",
  "education": "本科",
  "school": "测试大学",
  "major": "软件工程",
  "workExperience": "2024-2026 软件测试",
  "projectExperience": "招聘系统接口测试",
  "skills": "Java, MySQL, Postman",
  "selfIntroduction": "重视边界和异常测试"
}
```

更新时只有Body中出现的字段会覆盖。传入他人简历ID返回“无权修改他人简历”。

### `POST /resume/save`

**用途：** 传统表单方式新建/更新简历。

**认证：** 求职者Session。

**CSRF：** 必须传。

**Content-Type：** `application/x-www-form-urlencoded`。

**幂等：** 当前源码未标注。

参数与JSON接口基本一致，但`realName`、`phone`、`email`为必填；`id`为可选数字简历ID；还必须传`_csrf`。成功或失败都返回简历编辑HTML，不是JSON。

### 6.4 通知、会话与退出

### `GET /api/seeker/notifications`

**用途：** 查询当前求职者通知和未读数量。

**认证：** 求职者Session。

**参数：** 无。

`data.notifications[]`包括`id`、`type`、`content`、`relatedId`、`isRead`、`createdAt`；`data.unreadCount`为未读数。

### `GET /api/seeker/conversations`

**用途：** 查询求职者聊天会话。

**认证：** 求职者Session。

**参数：** 无。

每项包括`applicationId`、`conversationId`、`status`、`unread`、`lastMessage`、`jobTitle`、`companyName`、`otherUserId`、`otherUserName`。保存`conversationId`和`otherUserId`用于聊天接口。

### `POST /api/seeker/logout`

**用途：** JSON方式退出求职者会话并清除JWT/记住我Cookie。

**认证：** 普通用户Session。

**CSRF：** `/api/seeker/**`已排除。

**请求体：** 无。

```http
POST /api/seeker/logout HTTP/1.1
Host: superorange.top
Cookie: JSESSIONID={{JSESSIONID}}
```

成功返回`Result`；随后旧Session访问求职者接口应得到“请先以求职者身份登录”。

## 7. Boss、公司与职位管理接口

> 前置条件：使用`role=boss`的普通用户完成`POST /login`。除特别说明外，本节写请求需要同一Session的CSRF Token。

### 7.1 公司入驻与维护

### `POST /boss/register`

**用途：** Boss首次创建公司/完成入驻。

**认证：** Boss Session；非Boss会重定向首页。

**CSRF：** 必须传。

**幂等：** 必须传；同一Boss只允许一家公司。

**Content-Type：** `application/x-www-form-urlencoded`。

| 表单参数 | 类型 | 必填 | 说明 | 示例 |
|---|---|---:|---|---|
| `name` | string | 是 | 公司名称 | `微招测试公司` |
| `industry` | string | 否 | 行业 | `互联网` |
| `scale` | string | 否 | 公司规模 | `50-150人` |
| `address` | string | 否 | 地址；未传坐标时可能异步地理编码 | `北京市海淀区` |
| `description` | string | 否 | 公司介绍 | `接口练习公司` |
| `contactName` | string | 否 | 联系人 | `李经理` |
| `contactPhone` | string | 否 | 联系电话 | `13800138000` |
| `contactEmail` | string | 否 | 联系邮箱 | `hr@example.com` |
| `longitude` | number | 否 | 经度；通常与纬度同时传 | `116.397` |
| `latitude` | number | 否 | 纬度 | `39.908` |
| `_csrf` | string | 是 | 当前Session CSRF | `{{csrfToken}}` |

```http
POST /boss/register HTTP/1.1
Host: superorange.top
Cookie: JSESSIONID={{JSESSIONID}}
X-CSRF-Token: {{csrfToken}}
Idempotency-Key: {{idempotencyKey}}
Content-Type: application/x-www-form-urlencoded

name=微招测试公司&industry=互联网&scale=50-150人&address=北京市海淀区&contactName=李经理
```

成功：`302 Location: /boss`。重复入驻也会重定向`/boss`；服务异常时返回入驻HTML并显示错误。

### `POST /boss/company/edit`

**用途：** 修改当前Boss公司的资料。

**认证：** Boss Session。

**CSRF：** 必须传。

**幂等：** 当前源码未标注`@Idempotent`。

**Content-Type：** `application/x-www-form-urlencoded`。

可传字段：`industry`、`scale`、`address`、`description`、`contactName`、`contactPhone`、`contactEmail`、`longitude`、`latitude`。字段均可选；出现的字段覆盖原值。经纬度同时传时直接保存；只更新地址且原坐标为空时同步调用地理编码。

成功和失败都返回公司编辑HTML，通过页面文字判断结果。测试：未入驻Boss、只改一个字段、经纬度成对/缺一个、非法联系方式、缺少CSRF。

### 7.2 职位保存、关闭与重开

### `POST /boss/job/save`

**用途：** 新建或编辑职位；是否传数字`id`决定模式。

**认证：** Boss Session且已经入驻公司。

**CSRF：** 必须传。

**幂等：** 必须传。

**Content-Type：** `application/x-www-form-urlencoded`。

| 表单参数 | 类型 | 必填 | 说明 | 示例 |
|---|---|---:|---|---|
| `id` | integer | 编辑时 | 这里是数字职位ID，不是`encodedId` | `25` |
| `title` | string | 是 | 职位名称 | `Java测试开发` |
| `salaryMin` | integer | 否 | 最低薪资；不得高于最高薪资 | `15` |
| `salaryMax` | integer | 否 | 最高薪资 | `25` |
| `city` | string | 否 | 城市 | `北京` |
| `address` | string | 否 | 办公地址 | `海淀区` |
| `education` | string | 是 | 学历要求 | `本科` |
| `experience` | string | 是 | 经验要求 | `1-3年` |
| `description` | string | 是 | 职位描述 | `负责接口自动化测试` |
| `requirements` | string | 否 | 任职要求 | `熟悉Java和SQL` |
| `tags` | string | 否 | 标签文本 | `Java,接口测试` |
| `_csrf` | string | 是 | 当前Session CSRF | `{{csrfToken}}` |

新建成功时状态设为`active`；编辑保留原状态。成功重定向`/boss/jobs`。测试薪资上下界、编辑他人公司职位、未入驻、缺必填字段、相同幂等键重试。

### `POST /boss/job/delete/{encodedId}`

**用途：** 软关闭当前Boss公司的职位，实际把状态改为`closed`。

**认证：** Boss Session。

**CSRF：** 必须传。

**幂等：** 必须传。

**Path：** `encodedId={{encodedJobId}}`。

**请求体：** 无。

成功或多数失败均`302`回`/boss/jobs`。因此测试后应查询职位状态，而不能只看302。测试他人职位、无效ID、重复关闭。

### `POST /boss/job/reopen/{encodedId}`

**用途：** 重新开放状态为`closed`的职位。

**认证：** Boss Session。

**CSRF：** 必须传（路径匹配`/boss/**`）。

**幂等：** 必须传。

**Path：** `encodedId={{encodedJobId}}`。

**请求体：** 无。

成功：

```json
{"code":200,"msg":"success","data":{"status":"active"}}
```

常见失败：“参数无效”“无权操作此职位”“该职位未关闭，无需重开”。

### `GET /boss/job/{encodedId}/stats`

**用途：** 查询当前Boss职位的浏览和投递统计。

**认证：** Boss Session。

**Path：** `encodedId={{encodedJobId}}`。

**请求体：** 无。

成功`data`：

```json
{
  "viewCount": 120,
  "applyCount": 8,
  "statusBreakdown": {"pending": 2, "viewed": 3, "interviewing": 1, "rejected": 2}
}
```

测试他人公司职位和无效ID。

### 7.3 候选人和投递处理

### `GET /boss/resume/{encodedUserId}`

**用途：** Boss查看投递过自己公司职位的求职者简历。

**认证：** Boss Session。

**Path：** `encodedUserId`为求职者混淆ID，应从Boss投递页面的数据/链接取得。

**请求体：** 无。

成功`data.resume`为简历，`data.seekerName`为求职者显示名。没有向当前公司投递时返回业务403“无权查看此简历”，用于防止水平越权。

### `POST /boss/application/{encodedId}/status`

**用途：** 更新当前Boss收到的投递状态。

**认证：** Boss Session。

**CSRF：** 必须传。

**幂等：** 必须传。

**Content-Type：** Query参数或表单参数。

**Path：** `encodedId={{encodedApplicationId}}`。

| 参数 | 位置 | 类型 | 必填 | 规则/示例 |
|---|---|---|---:|---|
| `status` | Query/Form | string | 是 | `viewed`、`interviewing`、`offered`、`accepted`、`rejected` |
| `bossNote` | Query/Form | string | 否 | 给求职者的备注 |

```http
POST /boss/application/{{encodedApplicationId}}/status?status=viewed&bossNote=已查看简历 HTTP/1.1
Host: superorange.top
Cookie: JSESSIONID={{JSESSIONID}}
X-CSRF-Token: {{csrfToken}}
Idempotency-Key: {{idempotencyKey}}
```

成功后会创建求职者通知。测试非法状态、他人公司投递、无效ID、同键重试。

### `POST /boss/application/{encodedId}/interview`

**用途：** 安排面试，把投递状态更新为`interviewing`并通知求职者。

**认证：** Boss Session。

**CSRF：** 必须传。

**幂等：** 必须传。

**Content-Type：** `application/json`。

**Path：** `encodedId={{encodedApplicationId}}`。

| JSON字段 | 类型 | 必填 | 规则 | 示例 |
|---|---|---:|---|---|
| `interviewTime` | string | 否 | 非空时必须是ISO-8601本地时间 | `2026-07-15T14:30:00` |
| `interviewLocation` | string | 否 | 面试地点/会议链接 | `线上会议` |
| `bossNote` | string | 否 | 面试备注 | `请提前准备项目介绍` |

```json
{
  "interviewTime": "2026-07-15T14:30:00",
  "interviewLocation": "线上会议",
  "bossNote": "请提前准备项目介绍"
}
```

成功`data.status=interviewing`。错误时间格式会进入“操作失败”响应；他人公司投递返回无权操作。

## 8. 聊天、文件与通知接口

> 前置条件：普通用户已登录，且当前用户必须是相应投递会话的参与者。会话ID格式为`app_<数字投递ID>`，应直接使用会话列表响应中的`conversationId`。

### 8.1 文件

### `POST /chat/upload`

**用途：** 上传聊天中使用的PDF文件；上传成功后还需要调用发送消息接口创建文件消息。

**认证：** 普通用户Session且为会话参与者。

**CSRF：** 必须传，因为`/chat/**`受CSRF拦截。

**Content-Type：** `multipart/form-data`。

**幂等：** 当前源码未标注。

| 参数 | 位置 | 类型 | 必填 | 规则 |
|---|---|---|---:|---|
| `file` | multipart | binary | 是 | 最大10MB；文件名以`.pdf`结尾；真实内容头必须为`%PDF` |
| `conversationId` | multipart/query | string | 是 | 当前用户参与的会话，如`app_123` |
| `X-CSRF-Token` | Header | string | 是 | 当前Session Token |

```bash
curl -b cookie.txt \
  -H "X-CSRF-Token: {{csrfToken}}" \
  -F "file=@resume.pdf;type=application/pdf" \
  -F "conversationId={{conversationId}}" \
  http://superorange.top/chat/upload
```

成功`data`：

```json
{
  "fileName": "resume.pdf",
  "filePath": "/chat/20260710143000_a1b2c3d4.pdf",
  "fileSize": 102400
}
```

保存这三个字段并传给`POST /api/chat/send`。测试：超过10MB、伪装成PDF的文本、非PDF扩展名、无权会话、缺CSRF、空文件。

### `GET /chat/file/{storedName}`

**用途：** 下载自己参与会话中的PDF。

**认证：** 普通用户Session；必须是消息发送者或接收者。

**Path：** `storedName`为上传响应`filePath`去掉`/chat/`后的部分。

**响应：** `application/pdf`二进制附件。

```http
GET /chat/file/20260710143000_a1b2c3d4.pdf HTTP/1.1
Host: superorange.top
Cookie: JSESSIONID={{JSESSIONID}}
```

| HTTP | 含义 |
|---:|---|
| 200 | 有权限且文件存在 |
| 302 | 未登录，跳转`/login` |
| 403 | 路径不安全或当前用户不是收发双方 |
| 404 | 文件不存在或没有对应消息记录 |

### 8.2 消息

### `POST /api/chat/send`

**用途：** 通过HTTP发送文本或文件消息并触发WebSocket推送。

**认证：** 普通用户Session且为会话参与者。

**CSRF：** `/api/chat/**`已从CSRF拦截排除。

**幂等：** 必须传`Idempotency-Key`。

**限流：** 每用户60秒最多30次。

**Content-Type：** `application/json`。

| JSON字段 | 类型 | 必填 | 规则/说明 |
|---|---|---:|---|
| `conversationId` | string | 是 | 如`app_123` |
| `receiverId` | integer | 是 | 必须是会话另一方，不能是自己 |
| `content` | string | 文本消息是 | 会进行HTML清理；`text`类型不能为空 |
| `messageType` | string | 否 | `text`或`file`，默认`text` |
| `fileName` | string | 文件消息建议 | 上传接口返回的原文件名 |
| `filePath` | string | 文件消息建议 | 上传接口返回的路径 |
| `fileSize` | integer | 文件消息建议 | 字节数 |
| `clientMessageId` | string | 强烈建议 | 8–64位允许字符；同一发送者内唯一 |

文本消息示例：

```json
{
  "conversationId": "app_123",
  "receiverId": 2,
  "content": "你好，我想了解面试安排。",
  "messageType": "text",
  "clientMessageId": "550e8400-e29b-41d4-a716-446655440000"
}
```

文件消息示例：

```json
{
  "conversationId": "app_123",
  "receiverId": 2,
  "content": "",
  "messageType": "file",
  "fileName": "resume.pdf",
  "filePath": "/chat/20260710143000_a1b2c3d4.pdf",
  "fileSize": 102400,
  "clientMessageId": "file-20260710-0001"
}
```

成功`data`是保存后的消息，包含`id`、收发双方、内容/文件信息、`isRead`、`createdAt`。相同发送者重复使用同一合法`clientMessageId`时返回已有消息，不创建第二条。测试无效会话、给自己发、接收者不属于会话、空文本、非法类型、非法客户端ID、限流和同键重试。

### `GET /api/chat/messages/{conversationId}`

**用途：** 同步会话消息；用于补拉WebSocket连接窗口内可能遗漏的消息。

**认证：** 普通用户Session且为会话参与者。

**Path：** `conversationId`必填。

**Query：** `sinceId`可选，默认0；0或负数返回全部，大于0只返回ID更大的消息。

```http
GET /api/chat/messages/{{conversationId}}?sinceId=120 HTTP/1.1
Host: superorange.top
Cookie: JSESSIONID={{JSESSIONID}}
```

### `GET /api/chat/online-status/{userId}`

**用途：** 查询指定用户当前是否保持WebSocket在线。

**认证：** 普通用户Session。

**Path：** `userId`为数字用户ID。

```json
{"code":200,"msg":"success","data":{"userId":2,"online":true}}
```

当前接口只检查调用者已登录，没有检查被查询用户是否属于同一会话；这是练习权限测试时应记录的当前行为。

### `POST /api/chat/mark-read`

**用途：** 把当前会话中发给自己的未读消息标记为已读，并向发送者推送回执。

**认证：** 普通用户Session且为会话参与者。

**CSRF：** `/api/chat/**`已排除。

**幂等：** 当前源码未标注；重复调用结果自然保持已读。

**Content-Type：** `application/json`。

```json
{"conversationId":"app_123"}
```

测试缺少/空`conversationId`、他人会话、重复标记和未登录。

### 8.3 通知

### `POST /api/notifications/read-all`

**用途：** 把当前用户全部通知标记为已读。

**认证：** 普通用户Session。

**CSRF：** 必须传（`/api/**`受保护且此路径未被排除）。

**幂等：** 必须传。

**请求体：** 无。

```http
POST /api/notifications/read-all HTTP/1.1
Host: superorange.top
Cookie: JSESSIONID={{JSESSIONID}}
X-CSRF-Token: {{csrfToken}}
Idempotency-Key: {{idempotencyKey}}
```

### `POST /api/notifications/{id}/read`

**用途：** 标记一条属于当前用户的通知为已读。

**认证：** 普通用户Session。

**CSRF：** 必须传。

**幂等：** 必须传。

**Path：** `id`为`GET /api/seeker/notifications`返回的数字通知ID。

**请求体：** 无。

他人的通知ID返回业务403；不存在的ID由服务错误响应。练习前后比较`unreadCount`与目标通知`isRead`。

## 9. 管理后台接口

### 9.1 管理员权限矩阵

除`POST /api/admin/auth/login`外，全部管理员接口都必须传：

```http
Authorization: Bearer {{adminToken}}
```

管理员接口是无状态JWT认证，不使用普通用户`JSESSIONID`和`_csrf`。

| 路径族 | `super_admin` | `auditor` | `viewer` |
|---|:---:|:---:|:---:|
| `/api/admin/dashboard/**` | 允许 | 允许 | 允许 |
| `/api/admin/companies/**` | 允许 | 允许 | 禁止 |
| `/api/admin/jobs/**` | 允许 | 允许 | 禁止 |
| `/api/admin/users/**` | 允许 | 禁止 | 禁止 |
| `/api/admin/admins/**` | 允许 | 禁止 | 禁止 |
| `/api/admin/audit-logs/**` | 允许 | 允许 | 禁止 |
| `/api/admin/export/**` | 允许 | 允许 | 禁止 |

无Token/无效Token返回HTTP 401；Token有效但角色不足返回HTTP 403。

分页响应统一包含：`content`、`totalElements`、`totalPages`、`currentPage`、`pageSize`，页码从0开始。

### 9.2 仪表盘

### `GET /api/admin/dashboard/stats`

**用途：** 获取总用户、总职位、今日新增、待审核数及图表基础数据。

**权限：** 任意管理员。

**参数/请求体：** 无。

`data`字段：`totalUsers`、`totalJobs`、`todayNewUsers`、`pendingCount`、`userGrowth[]`、`jobDistribution[]`。

### `GET /api/admin/dashboard/charts`

**用途：** 单独获取用户增长和职位行业分布。

**权限：** 任意管理员。

**参数/请求体：** 无。

**响应：** `data.userGrowth[]`和`data.jobDistribution[]`。

### `GET /api/admin/dashboard/recent-logs`

**用途：** 获取最近审核/管理日志。

**权限：** `super_admin`或`auditor`。

**Query：** `limit`可选，integer，默认10。

**请求体：** 无。

```http
GET /api/admin/dashboard/recent-logs?limit=10 HTTP/1.1
Host: superorange.top
Authorization: Bearer {{adminToken}}
```

### 9.3 用户管理（仅超级管理员）

### `GET /api/admin/users`

**用途：** 分页筛选用户。

**权限：** `super_admin`。

| Query参数 | 类型 | 必填 | 默认/示例 |
|---|---|---:|---|
| `page` | integer | 否 | 默认0 |
| `size` | integer | 否 | 默认20 |
| `role` | string | 否 | `seeker`、`boss`、`admin` |
| `status` | string | 否 | `active`、`banned` |
| `keyword` | string | 否 | 用户名关键词 |

响应用户项：`id`、`username`、`nickname`、`phone`、`role`、`status`、`resumeCount`、`applicationCount`、`createdAt`。

### `GET /api/admin/users/{id}`

**用途：** 查询数字用户ID对应的详情和简历摘要。

**权限：** `super_admin`。

**Path：** `id`，integer，必填。

**响应：** 用户列表字段，加`email`、`resumeList[]`；简历摘要包含`id`、`realName`、`education`、`school`、`skills`。

### `PUT /api/admin/users/{id}/ban`

**用途：** 封禁用户。

**权限：** `super_admin`。

**幂等：** 必须传`Idempotency-Key`。

**Path：** 数字用户ID。

**请求体：** 无。

### `PUT /api/admin/users/{id}/unban`

**用途：** 解封用户。

**权限：** `super_admin`。

**幂等：** 必须传。

**Path：** 数字用户ID。

**请求体：** 无。

### `PUT /api/admin/users/{id}/reset-password`

**用途：** 管理员重置用户密码。

**权限：** `super_admin`。

**幂等：** 必须传。

**Content-Type：** `application/json`。

```json
{"newPassword":"ResetPass123"}
```

`newPassword`必填，遵守8–64位、大小写字母和数字规则，并且不能与目标用户名/手机号相同。测试空值、弱密码、目标用户不存在和同键重试。文档示例不是服务器真实密码。

管理员写请求通用示例：

```http
PUT /api/admin/users/12/ban HTTP/1.1
Host: superorange.top
Authorization: Bearer {{adminToken}}
Idempotency-Key: {{idempotencyKey}}
```

### 9.4 公司审核

### `GET /api/admin/companies`

**用途：** 分页查询待审核/已审核公司。

**权限：** `super_admin`或`auditor`。

| Query参数 | 类型 | 必填 | 默认/示例 |
|---|---|---:|---|
| `page` | integer | 否 | 默认0 |
| `size` | integer | 否 | 默认20 |
| `status` | string | 否 | `pending`、`approved`、`rejected` |
| `keyword` | string | 否 | 公司名称关键词 |

响应项：`id`、`name`、`industry`、`scale`、`address`、`description`、`bossName`、`auditStatus`、`auditReason`、`createdAt`。

### `GET /api/admin/companies/{id}`

**用途：** 查询数字公司ID详情。

**权限：** `super_admin`或`auditor`。

**Path：** `id`，integer，必填。

**请求体：** 无。

### `PUT /api/admin/companies/{id}/approve`

**用途：** 审核通过公司。

**权限：** `super_admin`或`auditor`。

**幂等：** 必须传。

**Path：** 数字公司ID。

**请求体：** 无。

### `PUT /api/admin/companies/{id}/reject`

**用途：** 驳回公司审核。

**权限：** `super_admin`或`auditor`。

**幂等：** 必须传。

**Content-Type：** `application/json`。

```json
{"reason":"公司资料不完整，请补充营业信息"}
```

测试：空原因、重复审核、不存在公司、auditor允许、viewer返回403。

### 9.5 职位审核

### `GET /api/admin/jobs`

**用途：** 分页筛选职位审核记录。

**权限：** `super_admin`或`auditor`。

| Query参数 | 类型 | 必填 | 默认/示例 |
|---|---|---:|---|
| `page` | integer | 否 | 默认0 |
| `size` | integer | 否 | 默认20 |
| `status` | string | 否 | `pending`、`approved`、`rejected` |
| `keyword` | string | 否 | 职位标题关键词 |

响应项：`id`、`title`、`companyName`、`companyId`、`salaryMin`、`salaryMax`、`city`、`education`、`experience`、`description`、`auditStatus`、`auditReason`、`createdAt`。

### `GET /api/admin/jobs/{id}`

**用途：** 查询数字职位ID详情。

**权限：** `super_admin`或`auditor`。

**Path：** `id`，integer，必填。

**请求体：** 无。

### `PUT /api/admin/jobs/{id}/approve`

**用途：** 审核通过职位。

**权限：** `super_admin`或`auditor`。

**幂等：** 必须传。

**Path：** 数字职位ID。

**请求体：** 无。

### `PUT /api/admin/jobs/{id}/reject`

**用途：** 驳回职位审核。

**权限：** `super_admin`或`auditor`。

**幂等：** 必须传。

**Content-Type：** `application/json`。

```json
{"reason":"职位描述不完整"}
```

### `POST /api/admin/jobs/batch-offline`

**用途：** 按数字职位ID批量下架。

**权限：** `super_admin`或`auditor`。

**幂等：** 必须传。

**Content-Type：** `application/json`。

```json
{"ids":[101,102,103]}
```

`ids`必填且至少一项；成功`data.successCount`表示实际成功数量。测试空数组、重复ID、不存在ID、部分已下架和同键重试。

### 9.6 子管理员管理（仅超级管理员）

### `GET /api/admin/admins`

**用途：** 查询全部子管理员及角色。

**权限：** `super_admin`。

**参数/请求体：** 无。

### `POST /api/admin/admins`

**用途：** 创建子管理员。

**权限：** `super_admin`。

**幂等：** 必须传。

**Content-Type：** `application/json`。

| JSON字段 | 类型 | 必填 | 规则 | 示例 |
|---|---|---:|---|---|
| `username` | string | 是 | 3–32位用户名规则 | `audit_practice` |
| `password` | string | 是 | 8–64位强密码 | `AuditPass123` |
| `roleType` | string | 是 | `super_admin`、`auditor`、`viewer` | `auditor` |

```json
{
  "username": "audit_practice",
  "password": "AuditPass123",
  "roleType": "auditor"
}
```

### `PUT /api/admin/admins/{userId}`

**用途：** 修改子管理员角色。

**权限：** `super_admin`。

**幂等：** 必须传。

**Path：** `userId`为数字用户ID。

**Content-Type：** `application/json`。

```json
{"roleType":"viewer"}
```

不能修改自己的角色。测试非法角色、自己、普通用户ID和不存在ID。

### `PUT /api/admin/admins/{userId}/disable`

**用途：** 禁用子管理员。

**权限：** `super_admin`。

**幂等：** 必须传。

**Path：** 数字用户ID。

**请求体：** 无。

**限制：** 不能禁用自己。

### 9.7 审计日志和导出

### `GET /api/admin/audit-logs`

**用途：** 分页查询管理员操作日志。

**权限：** `super_admin`或`auditor`。

| Query参数 | 类型 | 必填 | 格式/默认 |
|---|---|---:|---|
| `page` | integer | 否 | 默认0 |
| `size` | integer | 否 | 默认20 |
| `action` | string | 否 | 如`approve_company`、`ban_user` |
| `adminId` | integer | 否 | 操作管理员数字ID |
| `startDate` | string | 否 | ISO-8601，如`2026-07-01T00:00:00` |
| `endDate` | string | 否 | ISO-8601，如`2026-07-31T23:59:59` |

响应日志项：`id`、`adminId`、`adminName`、`action`、`targetType`、`targetId`、`reason`、`createdAt`。

### `GET /api/admin/export/users`

**用途：** 按条件导出用户CSV。

**权限：** `super_admin`或`auditor`。

**Query：** `role`、`status`、`keyword`均可选，与用户列表筛选含义一致。

**响应：** `text/csv; charset=UTF-8`，附件名`users.csv`。

在Apifox/Postman选择“保存响应到文件”，不要按JSON解析。

### `GET /api/admin/export/audit-logs`

**用途：** 按条件导出审计日志CSV。

**权限：** `super_admin`或`auditor`。

**Query：** `action`、`adminId`、`startDate`、`endDate`，格式同日志列表。

**响应：** CSV附件`audit_logs.csv`。

```http
GET /api/admin/export/audit-logs?startDate=2026-07-01T00:00:00&endDate=2026-07-31T23:59:59 HTTP/1.1
Host: superorange.top
Authorization: Bearer {{adminToken}}
```

## 10. 地图与公共数据接口

### `GET /api/geocode`

**用途：** 把地址转换为经纬度，供公司地址定位。

**认证：** 普通用户Session。未登录时响应体业务码401。

**请求体：** 无。

| Query参数 | 类型 | 必填 | 说明 | 示例 |
|---|---|---:|---|---|
| `address` | string | 是 | 详细地址 | `北京市海淀区中关村` |
| `city` | string | 否 | 城市提示 | `北京` |

```http
GET /api/geocode?address=北京市海淀区中关村&city=北京 HTTP/1.1
Host: superorange.top
Cookie: JSESSIONID={{JSESSIONID}}
```

成功：

```json
{"code":200,"msg":"success","data":{"lng":116.3,"lat":39.9}}
```

失败可能返回“请先登录”或“地理编码失败，请手动在地图上选择位置”。后者还可能表示上游地图服务不可用，不一定是参数格式错误。

## 11. 错误、限流与幂等

### 11.1 常见HTTP状态

| HTTP | 常见含义 | 本项目中的典型场景 |
|---:|---|---|
| 200 | 请求已被Controller处理 | JSON成功；也可能是业务失败`code!=200`；表单失败HTML |
| 302 | 重定向 | 登录成功到`/`；未登录/CSRF失败回`/login`；表单成功跳转 |
| 400 | 请求不合法 | Bean Validation失败、缺少/非法幂等键、参数类型转换失败 |
| 401 | 未认证 | 管理员Token缺失/过期；部分接口也可能HTTP 200且业务`code=401` |
| 403 | 权限不足 | 管理员角色不足、访问他人简历/通知/会话/文件 |
| 404 | 资源不存在 | 聊天文件不存在、错误路径 |
| 409 | 冲突 | 相同幂等键对应操作正在处理中 |
| 429 | 请求过频 | 验证码刷新、登录、注册、聊天等触发限流 |
| 500 | 未处理异常 | 服务、数据库或外部依赖异常；结合`journalctl`排查 |

### 11.2 限流练习

| 操作 | 当前规则/观察点 |
|---|---|
| 普通用户登录 | 同IP 60秒最多10次 |
| 注册 | 同IP 60秒最多3次 |
| 用户名检查 | 注解同IP每分钟30次；同Session内部5次后`rate_limited` |
| 验证码刷新 | 同Session 5秒冷却；每分钟频率限制；错误次数限制 |
| 聊天发送 | 每用户60秒最多30次 |

限流测试要使用练习账号，逐步增加次数并记录第一次出现429的位置；不要在一次练习后立刻把429误判为接口永久故障。

### 11.3 幂等响应

幂等拦截器的响应字段与普通`Result<T>`不同：

```json
{"code":409,"message":"操作处理中，请勿重复提交","duplicate":false}
```

```json
{"code":200,"message":"操作已完成","duplicate":true}
```

测试步骤：

1. 为一次业务操作生成`Idempotency-Key=A`。
2. 并发或快速发送两次相同请求，两个请求都使用A。
3. 验证只有一次业务变化；另一次为409或完成态重复响应。
4. 操作完成后继续用A重试，不能再新增第二条业务记录。
5. 真正发起新操作时改用B。

### 11.4 常见排查顺序

1. 检查请求方法、URL和`Content-Type`。
2. 检查参数位置：Path、Query、Header、Form和JSON不能混用。
3. 普通用户检查`JSESSIONID`与`_csrf`是否来自同一Session。
4. 登录/注册检查验证码是否在120秒内、是否属于同一Session。
5. 管理员检查`Authorization: Bearer`和角色。
6. 写请求检查`Idempotency-Key`。
7. 检查响应是JSON、HTML、图片、PDF还是CSV，不要使用错误解析器。
8. 服务端使用`sudo journalctl -u weib.service -f`查看实时日志。

## 12. 练习用例与变量记录表

### 12.1 推荐练习顺序

1. 管理员登录：最简单的JSON登录，理解`data.token`。
2. 管理员`/me`：理解Bearer Token。
3. 公开职位列表：练习Query、分页和`encodedId`。
4. 普通用户登录：练习Cookie Jar、CSRF、图片验证码和302。
5. 求职者简历：练习GET、JSON POST和Session。
6. 投递/收藏：练习CSRF和幂等键。
7. Boss公司/职位：练习表单和角色权限。
8. 聊天：练习JSON、文件上传、客户端消息ID和参与者权限。
9. 管理后台审核：练习JWT、RBAC和批量请求。
10. 异常/边界/限流：最后集中练习错误路径。

### 12.2 重点测试用例

| 编号 | 类型 | 接口/操作 | 输入变化 | 预期 |
|---|---|---|---|---|
| T01 | 正常 | `POST /api/admin/auth/login` | 正确管理员 | `data.token`非空 |
| T02 | 异常 | 管理员登录 | 错误密码 | 业务`code=401` |
| T03 | 认证 | `/api/admin/auth/me` | 无Token/损坏Token | HTTP 401 |
| T04 | 权限 | `/api/admin/users` | auditor或viewer Token | HTTP 403 |
| T05 | 正常 | 普通用户登录 | 同Session正确CSRF/验证码 | 302到`/` |
| T06 | 会话 | 普通用户登录 | 验证码和登录用不同Cookie | 验证码失败/跳转登录 |
| T07 | CSRF | 普通用户写请求 | 缺`_csrf`/请求头 | 302到登录 |
| T08 | 边界 | 注册用户名 | 2、3、32、33位 | 拒绝、接受、接受、拒绝 |
| T09 | 边界 | 新密码 | 7、8、64、65位 | 拒绝、按复杂度判断、接受、拒绝 |
| T10 | 格式 | 手机号 | 非1开头/10位/合法11位 | 拒绝、拒绝、接受 |
| T11 | 分页 | 求职者职位 | `page=-1`,`size=0`,`size=101` | 归一为0、12、100 |
| T12 | 权限 | Boss看简历 | 未向本公司投递的用户ID | 业务403 |
| T13 | 权限 | 撤回投递 | 他人投递ID | 拒绝 |
| T14 | 幂等 | 投递/审核 | 相同键并发两次 | 只执行一次 |
| T15 | 切换 | 收藏 | 不同键连续两次 | 收藏后取消 |
| T16 | 消息 | 文本消息 | 空`content` | 拒绝 |
| T17 | 消息 | 文件消息 | 伪PDF/超过10MB | 拒绝 |
| T18 | 消息 | `clientMessageId` | 同发送者重复 | 只保留一条 |
| T19 | 时间 | 安排面试 | 非ISO时间 | 操作失败 |
| T20 | 批量 | 批量下架 | `ids=[]` | “请选择要下架的职位” |
| T21 | 导出 | 用户CSV | 正确Token | CSV二进制，不按JSON解析 |
| T22 | 限流 | 验证码刷新 | 5秒内重复 | HTTP 429 |

### 12.3 变量记录表

复制下面内容到测试笔记，测试过程中逐项填写：

```text
baseUrl=http://superorange.top
JSESSIONID=
csrfToken=
captcha=
userJwt=
adminToken=
encodedJobId=
numericJobId=
encodedApplicationId=
encodedCompanyId=
conversationId=
receiverId=
notificationId=
adminUserId=
idempotencyKey=
clientMessageId=
```

### 12.4 完成标准

- 能独立完成普通用户和管理员两种登录。
- 能说明Cookie Session、JWT、CSRF、验证码和幂等键的区别。
- 能从前置响应取得`encodedId`，不把它和数字数据库ID混淆。
- 能判断HTTP状态、业务`code`、HTML重定向和二进制响应。
- 每个模块至少完成一个正常、一个异常、一个权限和一个边界用例。

## 12.5 投诉与处罚接口（可直接用于 Apifox/Postman）

以下标题采用 `METHOD /path` 格式，便于接口文档校验脚本和测试工具识别。

### `POST /api/complaints`
提交投诉，登录用户必填 `targetType`、`targetId`、`category`、`description`，可选 `evidenceUrls`。

### `GET /api/complaints/mine`
查询当前登录用户提交的投诉列表。

### `GET /api/complaints/{id}`
查询当前用户自己的投诉详情。

### `GET /api/admin/complaints`
管理员分页查询投诉，支持 `status`、`targetType`、`category`、`page`、`size`、`sort`。

### `GET /api/admin/complaints/{id}`
管理员查询投诉详情、证据和关联处罚。

### `POST /api/admin/complaints/{id}/reject`
管理员驳回投诉，请求体：`{"reason":"证据不足"}`。

### `POST /api/admin/complaints/{id}/resolve`
管理员处理投诉，可提交 `contentAction` 和 `sanction` 处罚对象。

### `GET /api/admin/sanctions`
管理员分页查询处罚记录，支持 `userId`、`sanctionType`、`activeOnly`、`page`、`size`、`sort`。

### `POST /api/admin/sanctions`
管理员直接创建处罚，请求体至少包含 `userId`、`sanctionType`、`reason`，可选 `targetType`、`targetId`、`endsAt`。

### `POST /api/admin/sanctions/{id}/revoke`
超级管理员撤销处罚，请求体：`{"reason":"复核后撤销"}`。

## 12.6 封禁申诉、管理员检索与论坛接口

### `POST /api/appeals`
提交当前账号处罚申诉，JSON：`{"sanctionId":1,"reason":"说明申诉理由","evidenceUrls":["/uploads/appeals/proof.png"]}`。账号受限用户仍可访问申诉页；同一处罚存在待审核申诉时返回 `409`。

### `GET /api/appeals/mine`
查询当前用户申诉，支持 `page`、`size`。

### `GET /api/appeals/{id}`
查询本人申诉详情。

### `POST /api/appeals/evidence`
以 multipart `file` 上传申诉图片，单张不超过 10MB，返回 `/uploads/appeals/` URL。

### `GET /api/admin/appeals`
管理员分页查询申诉，支持 `status`、`page`、`size`。

### `POST /api/admin/appeals/{id}/approve`
批准申诉并撤销对应处罚；请求体：`{"reason":"复核通过"}`。

### `POST /api/admin/appeals/{id}/reject`
驳回申诉；请求体：`{"reason":"证据不足"}`。

### `GET /api/admin/search`
管理员统一搜索，参数：`type=ALL|USER|COMPANY|JOB|RESUME`、`q`、`page`、`size`。

### `GET /api/admin/search/{type}/{id}`
查看搜索对象详情；敏感字段按管理员角色脱敏。

### `GET /api/forum/sections`
查询论坛板块。

### `GET /api/forum/posts`
查询帖子，支持 `sectionId`、`q`、`page`、`size`。

### `POST /api/forum/posts`
登录用户发布图文帖子，字段：`sectionId`、`title`、`content`、`tags`、`imageUrls`。

### `GET /api/forum/posts/{id}`
查询帖子详情，返回作者头像、发布时间、标签和互动计数。

### `POST /api/forum/posts/{id}/comments`
发表评论，JSON：`{"content":"评论内容"}`。

### `GET /api/forum/posts/{id}/comments`
查询帖子评论。

### `POST /api/forum/posts/{id}/like` / `DELETE /api/forum/posts/{id}/like`
点赞/取消点赞，重复点赞幂等。

### `POST /api/forum/posts/{id}/favorite` / `DELETE /api/forum/posts/{id}/favorite`
收藏/取消收藏，重复收藏幂等。

### `POST /api/forum/media`
上传论坛图片，单张不超过 10MB，仅支持 PNG/JPEG/GIF/WebP。

### Redis 与布局约定
论坛帖子/互动变更会删除 `cache:forum:post:*` 和 `cache:forum:posts:list:*`；所有前台页面统一引用 `/css/app-shell.css`，管理端统一使用 MUI theme 和 1200px PageContainer。

### `GET /api/admin/appeals/{id}`
管理员查看单条申诉及证据详情。

### `DELETE /api/forum/posts/{id}/like`
取消帖子点赞。

### `DELETE /api/forum/posts/{id}/favorite`
取消帖子收藏。

## 13. 页面型路由附录
### `GET /appeal`
处罚申诉页面，账号受限用户可访问。

### `GET /forum`
论坛首页，展示板块和帖子列表。

### `GET /forum/post/new`
论坛发帖页面。

### `GET /forum/post/{id}`
论坛帖子详情和评论页面。


以下路由主要返回HTML或管理后台SPA，不作为JSON业务接口。练习时可用于验证页面权限、Session和跳转。

### `GET /`

首页职位列表。可匿名访问，支持页面自身的搜索参数；主要业务查询建议使用`GET /api/seeker/jobs`。

### `GET /index`

首页别名，行为同`GET /`。

### `GET /login`

普通用户登录页。创建Session、生成CSRF并返回HTML；接口登录流程必须先调用它。

### `GET /register`

普通用户注册页。创建/复用Session并生成注册表单CSRF。

### `GET /change-password`

修改密码页。需要普通用户Session；未登录重定向`/login`。

### `GET /job/{encodedId}`

职位详情HTML页。`encodedId`为职位混淆ID。

### `GET /company/{encodedId}`

公司详情HTML页。`encodedId`为公司混淆ID。

### `GET /my/applications`

求职者投递记录HTML页；需要求职者Session。

### `GET /my/favorites`

求职者收藏HTML页；需要求职者Session。

### `GET /resume`

求职者简历编辑页；需要求职者Session。

### `GET /resume/preview`

简历预览页；需要普通用户Session。

### `GET /notifications`

通知HTML页；需要普通用户Session。

### `GET /chat`

聊天会话列表HTML页；需要普通用户Session。

### `GET /chat/{encodedId}`

指定投递会话的聊天HTML页；`encodedId`为投递混淆ID，且当前用户必须是参与者。

### `GET /boss`

Boss工作台首页；需要Boss Session。

### `GET /boss/register`

Boss公司入驻页；已入驻时重定向Boss首页。

### `GET /boss/jobs`

Boss职位管理页。

### `GET /boss/job/new`

Boss新建职位页。

### `GET /boss/job/edit/{encodedId}`

Boss编辑职位页；只能编辑本公司职位。

### `GET /boss/applications`

Boss收到的投递列表页；用于取得投递、求职者等页面数据。

### `GET /boss/company/edit`

Boss公司信息编辑页；未入驻时重定向入驻页。

### `GET /admin`

管理后台React SPA入口。

### `GET /admin/{path:[^.]+}`

管理后台一级前端路由回退到SPA入口，例如`/admin/users`。

### `GET /admin/{path1:[^.]+}/{path2:[^.]+}`

管理后台二级前端路由回退到SPA入口。

---

# 新增：投诉与处罚接口（2026-07-11）

## 用户端投诉接口

### POST `/api/complaints`

登录后提交投诉。请求体：

```json
{
  "targetType": "JOB",
  "targetId": 123,
  "category": "FAKE_JOB",
  "description": "职位薪资与实际不符",
  "evidenceUrls": ["/uploads/evidence.png"]
}
```

`targetType` 支持 `USER`、`JOB`、`COMPANY`、`RESUME`、`MEDIA`；`category` 支持 `FAKE_JOB`、`FAKE_PHOTO`、`FRAUD`、`HARASSMENT`、`SPAM`、`ILLEGAL`、`OTHER`。说明长度为 2-2000 个字符，证据最多 5 个，地址必须为 `/uploads/` 或 `http(s)://`。

响应：`200` 表示已进入 `PENDING` 审核；重复待审核投诉返回 `409`；未登录返回 `401`；参数错误返回 `400`。

### GET `/api/complaints/mine`

查询当前用户提交的投诉列表。

### GET `/api/complaints/{id}`

查询当前用户自己的投诉详情。查看他人投诉返回 `400`。

## 管理端投诉审核接口

管理端请求基地址为 `/api/admin`，需要管理员 JWT。审核员可以处理普通投诉；永久账号封禁和撤销处罚需要超级管理员。

| 方法 | 路径 | 请求体/参数 | 说明 |
|---|---|---|---|
| GET | `/complaints?status=PENDING&page=0&size=20` | 查询参数 | 分页查询投诉 |
| GET | `/complaints/{id}` | - | 投诉详情和证据 |
| POST | `/complaints/{id}/reject` | `{"reason":"证据不足"}` | 驳回投诉 |
| POST | `/complaints/{id}/resolve` | 见下方示例 | 通过投诉并可下架/处罚 |
| GET | `/sanctions?page=0&size=20` | 查询参数 | 处罚历史 |
| POST | `/sanctions` | 见下方示例 | 直接创建处罚 |
| POST | `/sanctions/{id}/revoke` | `{"reason":"复核后撤销"}` | 超级管理员撤销处罚 |

处理投诉示例：

```json
{
  "reason": "确认发布虚假职位",
  "contentAction": "OFFLINE",
  "sanction": {
    "userId": 20,
    "sanctionType": "PUBLISH_BAN",
    "targetType": "JOB",
    "targetId": 123,
    "sourceComplaintId": 1,
    "reason": "首次违规，禁止发布 7 天",
    "endsAt": "2026-07-18T12:00:00"
  }
}
```

`sanctionType` 支持 `MUTE`（禁言）、`PUBLISH_BAN`（禁止发布/编辑）、`ACCOUNT_BAN`（账号封禁）；`endsAt` 为空表示永久。投诉审核和处罚操作均写入 `audit_logs`，并发送站内通知。

## Redis 缓存行为

Boss、求职者公开资料、公司、职位、简历读取均先查 Redis，未命中才回源 MySQL 并回填。缓存使用 TTL 抖动、空值短缓存、单 Key 加载锁；数据库事务提交后立即删除相关 Key，并在约 800ms 后二次删除。Redis 故障时受控降级到 MySQL，密码、Token、验证码等敏感字段不会进入缓存。
