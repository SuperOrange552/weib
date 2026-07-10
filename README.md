# 微薄 (WeiBo) — 招聘平台

一个功能完整的招聘与求职平台，支持 Boss 直聘风格的投递和聊天交互，附带完整的管理后台。

---

## 项目描述

微薄是一个面向中小企业的在线招聘管理系统，包含求职者端、企业端（Boss 端）和管理后台三大模块。求职者可以浏览职位、投递简历、收藏职位、与 Boss 实时聊天；企业方可以发布职位、管理简历、查看候选人；管理后台提供用户管理、企业审核、职位审核、操作日志和仪表盘数据统计等功能。

项目采用前后端分离架构：后端基于 Spring Boot 3 + JPA + WebSocket + Redis，前端采用 Thymeleaf 模板（用户端）和 React 18 + MUI + Tailwind（管理后台）。

---

## 技术栈

### 后端
- **语言**: Java 17
- **框架**: Spring Boot 3.2.5
- **ORM**: Spring Data JPA + Hibernate
- **模板引擎**: Thymeleaf
- **数据库**: MySQL（生产）/ H2（开发）
- **缓存**: Redis + Spring Cache
- **安全**: Spring Security + JWT (HS256)
- **实时通信**: WebSocket (STOMP)
- **API 文档**: SpringDoc OpenAPI (Swagger)
- **构建工具**: Maven

### 前端（用户端）
- **模板**: Thymeleaf + Bootstrap
- **交互**: jQuery + STOMP.js (WebSocket 客户端)

### 前端（管理后台）
- **框架**: React 18 + TypeScript
- **UI 组件**: Material UI (MUI) v5
- **样式**: Tailwind CSS v3
- **构建工具**: Vite
- **HTTP 客户端**: Axios
- **图表**: Recharts
- **路由**: React Router v6
- **数据表格**: MUI Data Grid

### 其他
- **容器化**: 支持 Docker 部署
- **HTTPS**: 内嵌 SSL (keystore.p12)
- **CSV 导出**: 用户/操作日志导出
- **验证码**: 图形验证码

---

## 功能模块

### 求职者端
- 用户注册/登录（含验证码）
- 职位浏览与搜索（按城市、行业、关键词筛选）
- 投递简历（在线填写/上传简历文件）
- 收藏职位
- 与 Boss 实时聊天（WebSocket）
- 投递记录与状态跟踪

### 企业端（Boss）
- 企业注册与审核
- 职位发布与管理
- 查看候选人列表与简历
- 与求职者实时沟通
- 面试安排

### 管理后台
- **仪表盘**: 核心数据统计（用户数、职位数、待审核数、新增用户趋势）
- **企业审核**: 企业入驻资质审核（通过/驳回）
- **职位审核**: 职位内容审核（通过/驳回/批量下架）
- **用户管理**: 用户列表搜索查看、封禁/解封
- **子管理员**: 角色管理（super_admin / auditor / viewer）
- **操作日志**: 全量审计日志记录与搜索
- **CSV 导出**: 用户数据、操作日志导出
- **权限控制**: RBAC + JWT 双因子认证

---

## 适用范围

适用于以下场景：

- **中小企业内部招聘管理系统**：整合求职投递、企业管理和后台审核的全流程
- **招聘平台 MVP 原型**：可作为招聘类互联网产品的 0-1 原型代码
- **Spring Boot 全栈开发学习**：涵盖 JPA、Security、WebSocket、Redis、Thymeleaf、React 等主流技术整合
- **管理后台模板**：RBAC 权限系统 + React SPA 管理面板可复用至其他项目

---

## 快速开始

```bash
# 1. 克隆项目
git clone https://github.com/SuperOrange552/weib.git
cd weib

# 2. 配置数据库 (MySQL)
# 创建数据库: CREATE DATABASE weib;
# 修改 src/main/resources/application.yml 中的数据库连接

# 3. 启动后端
mvn spring-boot:run

# 4. 管理后台前端开发
cd admin-frontend
npm install
npm run dev

# 5. 构建管理后台
cd admin-frontend
npm run build
# 构建产物自动输出到 src/main/resources/static/admin/
```

---

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 超级管理员 | admin | Admin@123456 |


## API Testing Documentation

- 完整练习版（Markdown）：[`docs/API_TESTING_COMPLETE.md`](docs/API_TESTING_COMPLETE.md)
- 完整练习版（Word）：[`docs/微招系统完整接口测试文档.docx`](docs/微招系统完整接口测试文档.docx)
- 旧版简要参考：[`docs/API_TESTING.md`](docs/API_TESTING.md)
- Swagger UI：`http://superorange.top/swagger-ui/index.html`
- OpenAPI JSON：`http://superorange.top/v3/api-docs`
