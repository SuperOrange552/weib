# src/main/resources/application.yml

## 职责
Spring Boot 全局配置：HTTPS/SSL、数据库连接、Thymeleaf、文件上传、日志。

## 关键配置项
| 配置 | 值 | 说明 |
|------|-----|------|
| server.port | 8443 | HTTPS 端口 |
| server.ssl.key-store | classpath:keystore.p12 | SSL 证书 (PKCS12) |
| spring.datasource.url | jdbc:mysql://localhost:3306/weib | MySQL 数据库 |
| spring.jpa.hibernate.ddl-auto | update | 自动更新表结构 |
| spring.thymeleaf.cache | false | 开发模式不缓存 |
| storage.upload-dir | /opt/weib/uploads | 文件上传根目录 |

## 风险标记
- 数据库密码(123456)硬编码在配置文件中
- SSL 密码(weib123)硬编码
