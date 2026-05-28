# src/main/java/com/weib/entity/User.java

## 职责
用户实体，映射 `users` 表。含JSR-303校验注解和JPA生命周期回调。

## 表: users
| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | PK, IDENTITY自增 |
| username | String(50) | NOT NULL, UNIQUE, @NotBlank @Size(3,50) |
| password | String(100) | NOT NULL, @NotBlank @Size(6,100) |
| role | String(20) | "seeker"|"boss", 默认"seeker" |
| nickname | String(50) | 显示名称，默认同username |
| avatar | String(500) | 头像路径 |
| rememberToken | String(64) | Cookie自动登录令牌 |
| createdAt | LocalDateTime | @PrePersist自动设置 |
| updatedAt | LocalDateTime | @PrePersist/@PreUpdate自动设置 |

## 生命周期
- @PrePersist → onCreate(): 自动设置 createdAt, updatedAt
- @PreUpdate → onUpdate(): 自动设置 updatedAt

## 风险标记
- (无)
