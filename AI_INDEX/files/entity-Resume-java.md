# src/main/java/com/weib/entity/Resume.java

## 职责
简历实体，映射 `resumes` 表。每个用户(userId唯一)只有一份简历。

## 表: resumes
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | PK |
| userId | Long | UNIQUE, 用户ID |
| realName | String(50) | 真实姓名 |
| gender | String(10) | 性别 |
| phone | String(20) | 手机号 |
| email | String(100) | 邮箱 |
| birthday | String(20) | 生日 |
| education | String(20) | 最高学历 |
| school | String(100) | 学校 |
| major | String(100) | 专业 |
| workExperience | TEXT | 工作经历 |
| projectExperience | TEXT | 项目经验 |
| skills | String(1000) | 技能特长 |
| selfIntroduction | String(2000) | 自我介绍 |
| avatar | String(500) | 头像路径 |
| attachmentPath | String(500) | 附件路径 |
| status | String(20) | draft/active，默认draft |
| createdAt/updatedAt | LocalDateTime | 时间戳 |

## 生命周期
- @PrePersist → onCreate()
- @PreUpdate → onUpdate()

## 风险标记
- `large-file`: 15+字段
