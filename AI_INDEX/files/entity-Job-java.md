# src/main/java/com/weib/entity/Job.java

## 职责
职位实体，映射 `jobs` 表。

## 表: jobs
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | PK |
| title | String(100) | 职位名称 |
| companyId | Long | 公司ID(外键) |
| salaryMin/Max | Integer | 薪资范围(可选) |
| education | String(20) | 学历要求 |
| experience | String(20) | 经验要求 |
| city | String(50) | 城市 |
| address | String(200) | 详细地址 |
| description | TEXT | 职位描述 |
| requirements | TEXT | 任职要求 |
| tags | String(500) | 标签 |
| status | String(20) | active/closed，默认active |
| viewCount | Integer | 浏览量，默认0 |
| createdAt/updatedAt | LocalDateTime | 时间戳 |

## 生命周期
- @PrePersist → onCreate()
- @PreUpdate → onUpdate()

## 风险标记
- (无)
