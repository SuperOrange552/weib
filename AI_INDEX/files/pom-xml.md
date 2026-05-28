# pom.xml

## 职责
Maven 项目对象模型，管理 Spring Boot 3.2.5 项目的所有依赖和构建配置。

## 导出
- (无)

## 依赖
### 关键依赖
| 依赖 | 用途 |
|------|------|
| spring-boot-starter-web | Spring MVC + Tomcat + Jackson |
| spring-boot-starter-thymeleaf | 服务端 HTML 渲染 |
| spring-boot-starter-data-jpa | Hibernate + Spring Data JPA |
| spring-boot-starter-validation | Bean Validation (JSR-303) |
| spring-boot-starter-websocket | WebSocket 实时通讯 |
| spring-security-crypto | BCrypt 密码加密 (仅 crypto，不含完整 Security) |
| mysql-connector-j | MySQL JDBC 驱动 (runtime) |
| h2 | H2 内存数据库 (开发测试) |
| lombok | 减少样板代码 |

## 构建配置
- Java 17
- spring-boot-maven-plugin 打包可执行 JAR (排除 Lombok)

## 风险标记
- (无)
