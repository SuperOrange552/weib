# src/main/java/com/weib/config/LoginInterceptor.java

## 职责
全局登录拦截器：1) Session有效直接放行 2) Session无效时尝试 Cookie remember_token 自动登录 3) 都不行重定向/login。

## 导出
- `LoginInterceptor` — implements HandlerInterceptor

## 依赖
### 内部引用
- `UserRepository.findByRememberToken()` — 根据Cookie token查询用户
- `User` — 实体
### 外部依赖
- `jakarta.servlet.http.HttpServletRequest/Response/Session`

## 认证流程
```
preHandle(request, response, handler)
  ├─ Session有效 && user存在? → true (放行)
  ├─ Cookie remember_token? → DB查询 → 有效?
  │   └─ 创建新Session, setAttribute("user") → true (放行)
  └─ 都不满足 → response.sendRedirect("/login") → false (拦截)
```

## 风险标记
- (无)
