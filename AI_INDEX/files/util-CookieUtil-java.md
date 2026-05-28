# src/main/java/com/weib/util/CookieUtil.java

## 职责
Cookie操作工具：remember_token 的设置(HttpOnly, 2小时)和删除。

## 导出
- `CookieUtil.addRememberTokenCookie(HttpServletResponse, String)` — 设置token Cookie
- `CookieUtil.deleteRememberTokenCookie(HttpServletResponse)` — 清除token Cookie
- `REMEMBER_TOKEN_MAX_AGE` — 7200秒(2小时)

## Cookie配置
- Name: `remember_token`
- HttpOnly: true (防止XSS读取)
- Path: `/`
- MaxAge: 7200秒(2小时)

## 依赖
- `jakarta.servlet.http.Cookie`

## 风险标记
- (无)
