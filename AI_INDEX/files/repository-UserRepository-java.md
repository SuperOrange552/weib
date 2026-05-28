# src/main/java/com/weib/repository/UserRepository.java

## 职责
用户数据访问，extends JpaRepository<User, Long>。

## 自定义查询方法
| 方法 | 等价SQL |
|------|---------|
| findByUsername(String) | SELECT * FROM users WHERE username = ? |
| existsByUsername(String) | SELECT EXISTS(SELECT 1 FROM users WHERE username = ?) |
| findByRememberToken(String) | SELECT * FROM users WHERE remember_token = ? |

## 继承自 JpaRepository 的常用方法
save(), findById(), findAll(), deleteById(), count(), existsById()

## 风险标记
- (无)
