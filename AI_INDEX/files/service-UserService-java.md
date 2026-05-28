# src/main/java/com/weib/service/UserService.java

## 职责
用户核心业务逻辑：注册(含BCrypt加密+角色)、登录验证(BCrypt matches)、用户名存在检查、记住我令牌生成/清除。

## 导出
- `UserService` — 用户业务服务

## 依赖
### 内部引用
- `UserRepository` — findByUsername/existsByUsername/save/findByRememberToken
- `User` — 实体
### 外部依赖
- `PasswordEncoder` (BCryptPasswordEncoder) — encode/matches
- `@Transactional` — 事务管理

## 关键方法
| 方法 | 事务 | 说明 |
|------|------|------|
| register(String,String,String) | readWrite | 检查存在→BCrypt加密→保存 |
| login(String,String) | readOnly | 查用户→BCrypt匹配→返回Optional |
| existsByUsername(String) | readOnly | AJAX用户名实时检查 |
| generateRememberToken(User) | readWrite | UUID token→保存 |
| clearRememberToken(User) | readWrite | 清除token |

## 数据流
- 注册: username+password+role→exists检查→BCrypt encode→save→返回User(含生成ID)
- 登录: username→find→BCrypt matches→Optional.filter→返回Optional<User>

## 风险标记
- (无)
