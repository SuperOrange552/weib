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

> 本节将在后续接口清单中展开。

## 8. 聊天、文件与通知接口

> 本节将在后续接口清单中展开。

## 9. 管理后台接口

> 本节将在后续接口清单中展开。

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

> 本节将在后续测试清单中展开。

## 12. 练习用例与变量记录表

> 本节将在后续测试清单中展开。

## 13. 页面型路由附录

> 本节仅列出返回 HTML 的页面路由，后续补全。
