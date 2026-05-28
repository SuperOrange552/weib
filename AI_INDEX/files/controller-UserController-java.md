# src/main/java/com/weib/controller/UserController.java

## 职责
用户认证全流程：登录页/注册页展示、登录验证(含验证码+BCrypt)、注册(含验证码)、登出、用户名实时检查、记住我令牌。

## 导出
- `UserController` — 认证控制器

## 依赖
### 内部引用
- `UserService` — 认证/注册业务逻辑
- `CaptchaController.verify()` — 验证码校验 (静态方法调用)
- `CookieUtil` — remember_token Cookie 操作
### 外部依赖
- `org.springframework.stereotype.Controller`
- `jakarta.servlet.http.HttpSession`, `HttpServletResponse`

## 数据流
- 数据来源: 登录表单(username+password+captcha)、注册表单(含role)
- 数据传递: 验证成功 → Session.setAttribute("user", user) → 重定向首页
- 副作用: 登录成功生成 remember_token Cookie(2h); 登出销毁 Session + 清除 Cookie

## 端点
| 路由 | 方法 | 说明 |
|------|------|------|
| GET /login | loginPage() | 登录页面 |
| POST /login | login() | 登录验证 |
| GET /register | registerPage() | 注册页面 |
| POST /register | register() | 注册处理 |
| GET /logout | logout() | 登出 |
| GET /check-username | checkUsername() | AJAX 用户名检查 |

## 组件关系
- 父组件: ROOT (DispatcherServlet)
- 子组件: (无)

## 风险标记
- `large-file`: 507 行，含大量教学注释
