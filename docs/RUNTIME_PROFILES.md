# 运行环境配置

项目使用 Spring Profile 隔离本地 HTTPS 和服务器 HTTP，避免端口、证书、Cookie、WebSocket 配置互相污染。

## 本地 HTTPS

本地使用自签名证书和 `8443` 端口，同时监听 `8888` 并重定向到 HTTPS：

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:SSL_KEYSTORE_PASSWORD = "本地证书密码"
mvn spring-boot:run
```

访问：`https://localhost:8443`

## 服务器 HTTP

服务器由 Nginx 监听 `80`，Spring Boot 使用 `8888`：

```bash
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8888
```

Nginx 需要传递真实协议：

```nginx
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
```

论坛图片允许单张最大 10MB。生产 Nginx 必须为站点 `server` 或上传接口配置：

```nginx
client_max_body_size 12m;
```

12MB 为 10MB 文件加 multipart/form-data 边界和请求头预留空间；修改后执行
`sudo nginx -t && sudo systemctl reload nginx`。如果保持 Nginx 默认 1MB，上传会在到达 Spring Boot 前返回 HTTP 413。

访问：`http://superorange.top`

## 兼容规则

- Session Cookie：本地 HTTPS 使用 `Secure`，服务器 HTTP 不使用 `Secure`。
- JWT/Remember Cookie：根据当前请求及 `X-Forwarded-Proto` 动态设置 `Secure`。
- WebSocket/SockJS：使用相对路径 `/ws`，自动跟随页面的 HTTP/HTTPS 协议。
- 管理后台幂等键：优先使用 `crypto.randomUUID()`，不支持时自动降级。
