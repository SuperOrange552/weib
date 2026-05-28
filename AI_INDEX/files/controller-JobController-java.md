# src/main/java/com/weib/controller/JobController.java

## 职责
职位投递和投递记录查看。含 Result<T> 统一返回类。

## 导出
- `JobController` — 投递控制器
- `Result<T>` — 统一 JSON 响应格式 {code, msg, data}

## 依赖
### 内部引用
- `JobService` — 职位查询+已投递检查
- `ApplicationService` — 投递操作
- `User`, `Application`, `Job` — 实体
### 外部依赖
- Spring MVC, `jakarta.servlet.http.HttpSession`

## 端点
| 路由 | 方法 | 说明 |
|------|------|------|
| POST /job/{id}/apply | apply() | 投递职位(@ResponseBody,返回JSON) |
| GET /my/applications | myApplications() | 我的投递列表 |

## 数据流
- 投递: 检查登录→检查角色(seeker)→ApplicationService.apply()→校验已投递/简历/职位→创建记录
- 我的投递: 按userId查投递→关联查询职位名称→渲染模板

## Result<T> 返回格式
```json
{ "code": 200, "msg": "操作成功", "data": null }
```
状态码: 200(成功), 500(失败), 401(未登录), 403(无权限)

## 组件关系
- 父组件: ROOT
- 子组件: (无)

## 风险标记
- `many-exports`: 导出 Result<T> 辅助类
