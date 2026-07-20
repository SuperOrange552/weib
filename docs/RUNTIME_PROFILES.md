# 运行环境配置

项目使用 Spring Profile 隔离本地直连 HTTPS 和生产 Nginx TLS 终止，避免端口、证书、Cookie、WebSocket 配置互相污染。

## 本地 HTTPS

本地使用自签名证书和 `8443` 端口，同时监听 `8888` 并重定向到 HTTPS：

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:SSL_KEYSTORE_PASSWORD = "本地证书密码"
mvn spring-boot:run
```

访问：`https://localhost:8443`

## 生产服务器 HTTPS

服务器由 Nginx 监听 `80/443`：80 端口永久重定向到 HTTPS，443 端口使用 Let's Encrypt 证书并反向代理到 Spring Boot 内网 HTTP `8888`。Spring Boot 不直接管理公网证书：

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
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

论坛图片允许单张最大 10MB。生产 Nginx 必须为站点 `server` 或上传接口配置：

```nginx
client_max_body_size 12m;
```

12MB 为 10MB 文件加 multipart/form-data 边界和请求头预留空间；修改后执行
`sudo nginx -t && sudo systemctl reload nginx`。如果保持 Nginx 默认 1MB，上传会在到达 Spring Boot 前返回 HTTP 413。

生产环境变量：

```bash
WEBSOCKET_ALLOWED_ORIGINS=https://superorange.top,https://www.superorange.top
SERVER_SERVLET_SESSION_COOKIE_SECURE=true
```

证书自动续期应定期用 `sudo certbot renew --dry-run` 验证。

访问：`https://superorange.top`（`http://superorange.top` 自动 301 跳转）

## 兼容规则

- Session Cookie：本地和生产 HTTPS 均使用 `Secure`；生产必须正确传递 `X-Forwarded-Proto`。
- JWT/Remember Cookie：根据当前请求及 `X-Forwarded-Proto` 动态设置 `Secure`。
- WebSocket/SockJS：使用相对路径 `/ws`，自动跟随页面的 HTTP/HTTPS 协议。
- 管理后台幂等键：优先使用 `crypto.randomUUID()`，不支持时自动降级。

## API 自动化测试验证码开关

`GET /api/test/captcha` 仅用于受控的接口自动化环境，默认关闭。临时启用时设置：

```bash
TEST_CAPTCHA_API_ENABLED=true
TEST_CAPTCHA_ACCESS_KEY=<至少16位的随机密钥>
```

调用时传入 `X-Test-Access-Key`，响应 `data.captcha` 与当前 `JSESSIONID` 会话绑定。测试完成后把 `TEST_CAPTCHA_API_ENABLED` 改为 `false` 并重启 `weib.service`；关闭后 Controller 不注册，路径返回 404。密钥只保存在服务器环境变量中，禁止写入源码、接口文档或 Git。
