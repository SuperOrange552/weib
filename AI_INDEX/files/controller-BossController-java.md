# src/main/java/com/weib/controller/BossController.java

## 职责
Boss 端全功能控制器：入驻(创建公司)、职位 CRUD(发布/编辑/删除)、投递管理(查看/状态更新含水平越权防护)。

## 导出
- `BossController` — Boss 功能控制器

## 依赖
### 内部引用
- `JobService` — 职位管理
- `CompanyService` — 公司管理
- `ApplicationService` — 投递管理
- `User`, `Company`, `Job`, `Application` — 实体类
### 外部依赖
- Spring MVC (`@Controller`, `@GetMapping`, `@PostMapping`, `@ResponseBody`)
- `jakarta.servlet.http.HttpSession`

## 端点
| 路由 | 方法 | 说明 |
|------|------|------|
| GET /boss | bossHome() | Boss 首页+统计数据 |
| GET/POST /boss/register | 入驻页面/提交 | 创建公司 |
| GET /boss/jobs | manageJobs() | 职位管理列表 |
| GET /boss/job/new | newJob() | 发布职位页面 |
| GET /boss/job/edit/{id} | editJob() | 编辑职位(含权限检查) |
| POST /boss/job/save | saveJob() | 新建/编辑共用一个方法 |
| POST /boss/job/delete/{id} | deleteJob() | 软删除(设置为closed) |
| GET /boss/applications | applications() | 查看所有投递 |
| POST /boss/application/{id}/status | updateApplicationStatus() | 更新状态(含越权防护) |

## 安全要点
- 所有端点检查登录+角色(boss)
- 编辑/删除职位前验证：职位须属于当前Boss的公司
- 更新投递状态前：投递→职位→公司 三级校验

## 组件关系
- 父组件: ROOT
- 子组件: (无)

## 风险标记
- `large-file`: 590 行
