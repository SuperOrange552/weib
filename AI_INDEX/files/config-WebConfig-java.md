# src/main/java/com/weib/config/WebConfig.java

## 职责
Spring MVC 全局配置：注册 LoginInterceptor 拦截器(白名单排除静态资源/登录/注册/验证码)、上传文件和聊天文件的静态资源映射。

## 导出
- `WebConfig` — implements WebMvcConfigurer

## 依赖
### 内部引用
- `LoginInterceptor` — 登录拦截器
- `storage.upload-dir`, `storage.chat-dir` — @Value注入
### 外部依赖
- `org.springframework.web.servlet.config.annotation`

## 拦截器白名单（不拦截）
`/login`, `/register`, `/captcha`, `/check-username`, `/uploads/**`, `/chat/**`, `/ws/**`, `/css/**`, `/js/**`, `/images/**`, `/static/**`

## 静态资源映射
- `/uploads/**` → `file:${storage.upload-dir}/`
- `/chat/**` → `file:${storage.chat-dir}/`

## 风险标记
- (无)
