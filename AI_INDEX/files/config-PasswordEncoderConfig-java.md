# src/main/java/com/weib/config/PasswordEncoderConfig.java

## 职责
注册 BCryptPasswordEncoder Bean，供 UserService 注入使用。

## 导出
- `PasswordEncoderConfig` — @Configuration
- `PasswordEncoder` (Bean) — BCryptPasswordEncoder(strength=10)

## 依赖
### 外部依赖
- `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`

## 使用场景
- 注册: `passwordEncoder.encode(rawPassword)` → 存库
- 登录: `passwordEncoder.matches(rawPassword, encodedPassword)` → 比对

## 风险标记
- (无)
