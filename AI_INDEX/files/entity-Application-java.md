# src/main/java/com/weib/entity/Application.java

## 职责
投递记录实体，映射 `applications` 表。关联求职者、职位、简历。

## 表: applications
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | PK |
| jobId | Long | 职位ID (FK→jobs) |
| userId | Long | 求职者ID (FK→users) |
| resumeId | Long | 使用的简历ID (FK→resumes) |
| status | String(20) | pending/viewed/interview/accepted/rejected，默认pending |
| bossNote | String(500) | Boss备注 |
| createdAt/updatedAt | LocalDateTime | 时间戳 |

## @Transient字段
- `jobTitle` — 不存入数据库，Controller中临时设置用于模板显示

## 生命周期
- @PrePersist → onCreate()

## 风险标记
- (无)
