# src/main/java/com/weib/controller/IndexController.java

## 职责
网站首页（职位列表+搜索筛选）和职位详情页。

## 导出
- `IndexController` — 首页和职位详情控制器

## 依赖
### 内部引用
- `JobService` — 职位查询/搜索/浏览量+1/已投递检查
- `CompanyService` — 公司信息查询
### 外部依赖
- `org.springframework.stereotype.Controller`
- `org.springframework.ui.Model`
- `jakarta.servlet.http.HttpSession`

## API 调用
- 高德地图静态图 API: `https://restapi.amap.com/v3/staticmap` (职位详情页地图打点)

## 数据流
- 数据来源: 数据库(Job/Company) + Session(User)
- 数据传递: Controller → Model → Thymeleaf 模板
- 副作用: 职位详情页触发 `incrementViewCount()`

## 端点
| 路由 | 方法 | 模板 |
|------|------|------|
| GET /, /index | index() | index.html |
| GET /job/{id} | jobDetail() | job-detail.html |

## 组件关系
- 父组件: ROOT (DispatcherServlet 路由)
- 子组件: (无，返回 Thymeleaf 视图)

## 风险标记
- `large-file`: 497 行，含冗长中文注释
