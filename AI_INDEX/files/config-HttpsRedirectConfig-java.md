# src/main/java/com/weib/config/HttpsRedirectConfig.java

## 职责
Tomcat容器定制：添加额外HTTP连接器(8080端口)，自动302重定向到HTTPS(8443端口)。

## 导出
- `HttpsRedirectConfig` — @Configuration
- `WebServerFactoryCustomizer<TomcatServletWebServerFactory>` (Bean)

## 配置
- HTTP端口: 8080 (scheme="http", secure=false)
- 重定向目标: 8443 (setRedirectPort)

## 数据流
用户访问 http://localhost:8080 → Tomcat Connector 302 → https://localhost:8443

## 风险标记
- (无)
