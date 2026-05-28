# src/main/java/com/weib/controller/ResumeController.java

## 职责
求职者简历全生命周期管理：查看/新建/编辑/保存(含水平越权防护)/预览。

## 导出
- `ResumeController` — 简历控制器

## 依赖
### 内部引用
- `ResumeService` — 简历 CRUD
- `Resume`, `User` — 实体
### 外部依赖
- Spring MVC, `jakarta.servlet.http.HttpSession`

## 端点
| 路由 | 方法 | 说明 |
|------|------|------|
| GET /resume | myResume() | 简历编辑页(无简历则显示空表单) |
| POST /resume/save | saveResume() | 保存简历(新建/更新) |
| GET /resume/preview | previewResume() | 简历预览页 |

## 数据流
- 查看/编辑: 按userId查简历→有则编辑模式/无则新建模式→渲染 resume-edit.html
- 保存: 构建Resume对象→设置所有字段→ResumeService.saveResume()→返回编辑页
- 预览: 按userId查简历→渲染 resume-preview.html

## 安全要点
- 保存(更新)时验证 `resume.userId == session.user.id`，防止篡改ID越权修改他人简历

## 组件关系
- 父组件: ROOT
- 子组件: (无)

## 风险标记
- (无)
