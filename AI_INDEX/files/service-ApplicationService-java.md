# src/main/java/com/weib/service/ApplicationService.java

## 职责
投递核心业务逻辑：投递(三重校验)→状态更新→多维度查询(按用户/职位/公司/ID)。

## 导出
- `ApplicationService` — 投递业务服务

## 依赖
### 内部引用
- `ApplicationRepository` — 投递数据访问
- `JobRepository` — 职位查询(校验职位存在)
- `ResumeRepository` — 简历查询(校验简历存在)
- `Application`, `Job`, `Resume` — 实体
### 外部依赖
- `@Transactional`

## 关键方法
| 方法 | 事务 | 说明 |
|------|------|------|
| apply(Long jobId, Long userId) | readWrite | 三重校验→创建投递记录 |
| getApplicationsByUser(Long) | readOnly | 求职者查看自己的投递 |
| getApplicationsByJob(Long) | readOnly | Boss查看职位的投递 |
| getApplicationsByCompany(Long) | readOnly | 聚合查询公司所有投递 |
| getApplicationById(Long) | readOnly | 按ID查询(用于权限校验) |
| updateStatus(Long,String,String) | readWrite | Boss更新状态+备注 |
| hasApplied(Long,Long) | readOnly | 去重检查 |

## 投递三重校验流程
1. `hasApplied(jobId, userId)` → 已投递抛异常 "您已投递过该职位"
2. `resumeRepository.findByUserId(userId)` → 无简历抛异常 "请先完善简历"
3. `jobRepository.findById(jobId)` → 不存在抛异常 "职位不存在"

## 组件关系
- 父组件: BossController, JobController
- 子组件: ApplicationRepository, JobRepository, ResumeRepository

## 风险标记
- (无)
