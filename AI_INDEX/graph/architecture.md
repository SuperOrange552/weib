# weib — 项目架构总览

## 项目简介
**微薄 (weib)** — Spring Boot 3.2.5 在线招聘平台，Java 17 + Thymeleaf 服务端渲染 + WebSocket 实时聊天。

## 技术栈
| 层面 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.5, Spring MVC, Spring Data JPA, Spring WebSocket |
| 模板引擎 | Thymeleaf (服务端渲染 HTML) |
| 数据库 | MySQL (生产) / H2 (开发), Hibernate ORM |
| 安全 | BCrypt 密码加密, HTTPS(8443)+HTTP→HTTPS 重定向, Session+Cookie 认证 |
| 构建 | Maven, Java 17 |
| 其他 | Lombok, Bean Validation, 高德地图静态图 API |

## 三层架构
```
Controller (7) → Service (6) → Repository (6) → JPA/Hibernate → MySQL
     ↑                               ↑
     └── Entity (6) ─────────────────┘
     ↑
     └── Util (2), Config (5), Exception (1)
```

## 模块依赖关系
```
controller ← service ← repository ← entity
    ↑            ↑           ↑
    ├── util     ├── entity  ├── entity
    └── entity   └── repository
```

## 核心业务流

### 求职者流程
1. 注册 → 登录 → 浏览职位(首页搜索/筛选) → 查看职位详情 → 完善简历 → 投递职位 → 查看投递状态 → 与Boss实时聊天

### Boss流程
1. 注册 → 登录 → 入驻(创建公司) → 发布职位 → 管理职位 → 查看投递 → 处理申请 → 与求职者实时聊天

## 关键文件索引

| 文件 | 职责 | 优先级 |
|------|------|--------|
| WeibApplication.java | 启动入口 | strict |
| controller/IndexController.java | 首页+职位详情 | strict |
| controller/UserController.java | 登录/注册/登出 | strict |
| controller/BossController.java | Boss端全功能 | strict |
| controller/JobController.java | 投递+投递记录 | strict |
| controller/ResumeController.java | 简历管理 | strict |
| controller/ChatController.java | WebSocket聊天+文件 | strict |
| controller/CaptchaController.java | 验证码 | strict |
| service/UserService.java | 用户认证逻辑 | strict |
| service/JobService.java | 职位CRUD+搜索 | strict |
| service/ApplicationService.java | 投递核心逻辑 | strict |
| config/WebConfig.java | 拦截器+资源映射 | strict |
| config/LoginInterceptor.java | 认证拦截 | strict |
| config/WebSocketConfig.java | WebSocket配置 | strict |
| entity/User.java | 用户表 | strict |
| entity/Job.java | 职位表 | strict |
| entity/Application.java | 投递表 | strict |
| application.yml | 全局配置 | strict |
| pom.xml | 依赖管理 | strict |
| seed_data.sql | 测试数据 | loose |

## Session 认证机制
1. 用户登录 → `session.setAttribute("user", user)`
2. LoginInterceptor 拦截所有请求（白名单除外）
3. Session 无效时自动降级到 Cookie `remember_token` (2小时有效期)
4. 登出 → `session.invalidate()` + 清除 Cookie

## WebSocket 聊天
- STOMP over WebSocket, SockJS 回退
- 端点: `/ws`
- 消息代理: `/topic`(广播), `/queue`(点对点)
- 会话ID: `app_{applicationId}`
- 支持文件(PDF)传输
