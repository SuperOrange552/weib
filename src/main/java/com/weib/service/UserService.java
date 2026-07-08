package com.weib.service;

import com.weib.entity.User;
import com.weib.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ============================================
 * 【Service 类】业务逻辑层
 * ============================================
 * 
 * 什么是 Service？
 * - Service = 业务服务，负责处理业务逻辑
 * - 是三层架构的中间层：Controller → Service → Repository
 * - Controller 只负责接收请求，Service 负责处理逻辑
 * 
 * 【为什么需要 Service 层？】
 * - 分离关注点：Controller 处理请求，Service 处理业务
 * - 复用逻辑：多个 Controller 可以调用同一个 Service
 * - 事务管理：Service 层统一管理事务
 * - 测试方便：可以单独测试 Service
 * 
 * ----------------------------------------
 * 【@Service】Spring 组件注解
 * ----------------------------------------
 * 
 * 作用：标记这个类是业务服务组件
 * 
 * 特点：
 * - 是 @Component 的衍生注解
 * - 被 @ComponentScan 自动扫描并注册为 Bean
 * - 语义上表示这是一个业务服务
 * 
 * 【@Service vs @Component】
 * - 功能上完全一样
 * - @Service 语义更清晰
 * - IDE 和工具可以根据注解做特殊处理
 * 
 * ----------------------------------------
 * 【@RequiredArgsConstructor】Lombok 注解
 * ----------------------------------------
 * 
 * 作用：自动生成包含所有 final 字段的构造函数
 * 
 * 等价代码：
 * public UserService(UserRepository userRepository) {
 *     this.userRepository = userRepository;
 * }
 * 
 * 【为什么用 final + 构造器注入？】
 * 
 * 这是 Spring 推荐的依赖注入方式！
 * 
 * 依赖注入的三种方式：
 * 
 * 1. 字段注入（不推荐）
 *    @Autowired
 *    private UserRepository userRepository;
 *    
 *    缺点：
 *    - 不能用于 final 字段
 *    - 难以测试（需要反射）
 *    - 隐藏依赖关系
 * 
 * 2. Setter 注入（可选依赖时用）
 *    private UserRepository userRepository;
 *    
 *    @Autowired
 *    public void setUserRepository(UserRepository userRepository) {
 *        this.userRepository = userRepository;
 *    }
 *    
 *    缺点：
 *    - 依赖可以在运行时被改变
 *    - 不能用于 final 字段
 * 
 * 3. 构造器注入（推荐）
 *    private final UserRepository userRepository;
 *    
 *    public UserService(UserRepository userRepository) {
 *        this.userRepository = userRepository;
 *    }
 *    
 *    优点：
 *    - 可以用于 final 字段（不可变）
 *    - 依赖关系明确（看构造函数就知道需要什么）
 *    - 易于测试（可以直接 new）
 *    - 不需要 @Autowired（Spring 4.3+ 单构造器自动注入）
 * 
 * 【Lombok 简化】
 * @RequiredArgsConstructor
 * public class UserService {
 *     private final UserRepository userRepository;
 * }
 * 
 * 一行搞定构造器注入！
 * 
 * ----------------------------------------
 * 【@Transactional】事务管理注解
 * ----------------------------------------
 * 
 * 作用：声明式事务管理，自动开启/提交/回滚事务
 * 
 * 使用位置：类上（所有 public 方法）或方法上（单个方法）
 * 
 * 【什么是事务？】
 * 事务是一组操作，要么全部成功，要么全部失败：
 * - 原子性（Atomicity）：不可分割
 * - 一致性（Consistency）：从一个一致状态到另一个一致状态
 * - 隔离性（Isolation）：多个事务互不干扰
 * - 持久性（Durability）：提交后永久保存
 * 
 * 【经典例子：转账】
 * A 转账给 B 100元：
 * 1. A 余额减 100
 * 2. B 余额加 100
 * 
 * 如果第2步失败，第1步必须回滚！否则 A 的钱就凭空消失了。
 * 
 * 【@Transactional 属性】
 * 
 * 1. readOnly（是否只读）
 *    - true：只读事务，不修改数据
 *    - 优化：数据库可以进行优化
 *    - 用在查询方法上
 * 
 * 2. timeout（超时时间）
 *    - 单位：秒
 *    - 超时自动回滚
 * 
 * 3. rollbackFor（回滚异常）
 *    - 默认只回滚 RuntimeException 和 Error
 *    - rollbackFor = Exception.class 可以回滚所有异常
 * 
 * 4. propagation（传播行为）
 *    - REQUIRED（默认）：有事务就加入，没有就新建
 *    - REQUIRES_NEW：总是新建事务
 *    - SUPPORTS：有事务就加入，没有就非事务执行
 * 
 * 5. isolation（隔离级别）
 *    - READ_UNCOMMITTED：读未提交
 *    - READ_COMMITTED：读已提交
 *    - REPEATABLE_READ：可重复读（MySQL 默认）
 *    - SERIALIZABLE：串行化
 * 
 * 【使用建议】
 * - 类上加 @Transactional（所有方法默认开启事务）
 * - 查询方法加 @Transactional(readOnly = true)
 * - 不要在 private 方法上加（无效）
 * - 不要在接口上加（虽然可以，但不推荐）
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    /**
     * UserRepository - 用户数据访问
     * PasswordEncoder - BCrypt 密码加密器
     */
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户注册
     * 
     * ----------------------------------------
     * 【方法功能】
     * ----------------------------------------
     * 
     * 1. 检查用户名是否已存在
     * 2. 创建用户对象
     * 3. 保存到数据库
     * 4. 返回用户对象
     * 
     * ----------------------------------------
     * 【为什么要这么写？】
     * ----------------------------------------
     * 
     * 1. 先检查再保存（避免重复）
     *    - 如果直接 save，数据库会报 UNIQUE 约束错误
     *    - 提前检查可以给出友好的错误提示
     * 
     * 2. 返回 null 表示失败
     *    - 简单直接
     *    - 实际项目建议抛出自定义异常或返回 Result 对象
     * 
     * ----------------------------------------
     * 【执行流程】
     * ----------------------------------------
     * 
     * 1. Controller 调用 register("admin", "123456")
     * 2. 检查用户名是否存在
     *    - SELECT EXISTS(SELECT 1 FROM users WHERE username = 'admin')
     * 3. 如果存在，返回 null
     * 4. 如果不存在，创建 User 对象
     * 5. 调用 userRepository.save(user)
     *    - Hibernate 生成 ID
     *    - 执行 @PrePersist 回调
     *    - INSERT INTO users (username, password, ...) VALUES (?, ?, ...)
     * 6. 返回保存后的 User（包含生成的 ID）
     * 
     * ----------------------------------------
     * 【事务说明】
     * ----------------------------------------
     * 
     * - 方法上有 @Transactional（继承自类）
     * - 如果保存失败，自动回滚
     * 
     * 【安全提示】
     * 实际项目中密码应该加密！
     * 
     * // 使用 BCrypt 加密
     * @Autowired
     * private PasswordEncoder passwordEncoder;
     * 
     * user.setPassword(passwordEncoder.encode(password));
     * 
     * @param username 用户名
     * @param password 密码（明文，实际项目应加密）
     * @return 注册成功返回用户对象，失败返回 null
     */
    public User register(String username, String password) {
        return register(username, password, null, "seeker");
    }
    
    /**
     * 用户注册（支持指定角色）
     * 
     * ----------------------------------------
     * 【重载方法】register(String, String)
     * ----------------------------------------
     * 
     * Java 支持方法重载（Overload）：
     * - 方法名相同，参数不同
     * - 调用时根据参数自动匹配
     * 
     * register("admin", "123456")
     * → 调用 register(String, String)
     * → role 默认是 "seeker"
     * 
     * register("boss1", "123456", "boss")
     * → 调用 register(String, String, String)
     * → role 是传入的 "boss"
     * 
     * @param username 用户名
     * @param password 密码
     * @param role 用户角色（seeker=求职者，boss=Boss）
     * @return 注册成功返回用户对象，失败返回 null
     */
    public User register(String username, String password, String phone, String role) {
        String usernameError = com.weib.security.CredentialPolicy.validateUsername(username);
        if (usernameError != null) throw new IllegalArgumentException(usernameError);
        String passwordError = com.weib.security.CredentialPolicy.validatePassword(password, username, phone);
        if (passwordError != null) throw new IllegalArgumentException(passwordError);
        username = com.weib.security.CredentialPolicy.normalizeUsername(username);
        if (userRepository.existsByUsername(username)) {
            return null;
        }
        if (phone != null && !phone.isBlank() && userRepository.existsByPhone(phone)) {
            return null;
        }

        User user = new User();
        user.setUsername(com.weib.security.CredentialPolicy.normalizeUsername(username));
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone != null && !phone.isBlank() ? phone : null);
        user.setRole(role);
        user.setNickname(username);

        return userRepository.save(user);
    }

    /**
     * 验证密码是否符合规则：长度>=7，必须包含大小写字母和数字
     */
    public static String validatePassword(String password) {
        return com.weib.security.CredentialPolicy.validatePassword(password);
    }

    /**
     * 用户登录验证
     * 
     * ----------------------------------------
     * 【方法功能】
     * ----------------------------------------
     * 
     * 1. 根据用户名查询用户
     * 2. 如果存在，验证密码
     * 3. 密码匹配则返回用户，否则返回空
     * 
     * ----------------------------------------
     * 【为什么要这么写？】
     * ----------------------------------------
     * 
     * 1. 使用 Optional 的函数式操作
     *    - findByUsername() 返回 Optional<User>
     *    - filter() 过滤密码不匹配的
     *    - 一行代码完成查询+验证
     * 
     * 2. 为什么不直接返回 boolean？
     *    - 登录成功后需要用户信息（如用户名、ID）
     *    - 返回 User 对象方便后续使用
     * 
     * ----------------------------------------
     * 【执行流程】
     * ----------------------------------------
     * 
     * 1. Controller 调用 login("admin", "123456")
     * 2. 查询用户
     *    - SELECT * FROM users WHERE username = 'admin'
     * 3. 如果用户不存在
     *    - Optional 为空
     *    - filter 不执行
     *    - 返回 Optional.empty()
     * 4. 如果用户存在
     *    - Optional 有值
     *    - filter 验证密码
     *    - 密码匹配：返回 Optional.of(user)
     *    - 密码不匹配：返回 Optional.empty()
     * 
     * ----------------------------------------
     * 【Optional.filter() 原理】
     * ----------------------------------------
     * 
     * Optional<User> opt = Optional.of(user);
     * 
     * opt.filter(user -> user.getPassword().equals(password))
     * 
     * 等价于：
     * if (opt.isPresent()) {
     *     User user = opt.get();
     *     if (user.getPassword().equals(password)) {
     *         return Optional.of(user);
     *     } else {
     *         return Optional.empty();
     *     }
     * } else {
     *     return Optional.empty();
     * }
     * 
     * 【安全提示】
     * 实际项目中密码验证应该用：
     * passwordEncoder.matches(inputPassword, user.getPassword())
     * 
     * @param username 用户名
     * @param password 密码
     * @return 登录成功返回 Optional.of(user)，失败返回 Optional.empty()
     */
    // 修复：login 方法需要写操作（更新 loginFailCount/lockUntil），不能用 readOnly
    @Transactional
    public Optional<User> login(String account, String password) {
        // 先按用户名查找
        Optional<User> userOpt = userRepository.findByUsername(account);
        // 如果没找到，按手机号查找
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhone(account);
        }
        if (userOpt.isEmpty()) return Optional.empty();

        User user = userOpt.get();

        // 检查账户是否被锁定
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            return Optional.empty(); // 账户已锁定
        }

        // 检查管理员是否已封禁该用户
        if ("banned".equals(user.getStatus())) {
            return Optional.empty(); // 已被管理员封禁
        }

        // 验证密码
        if (passwordEncoder.matches(password, user.getPassword())) {
            // 登录成功，重置失败计数
            if (user.getLoginFailCount() != null && user.getLoginFailCount() > 0) {
                user.setLoginFailCount(0);
                user.setLockUntil(null);
                userRepository.save(user);
            }
            return Optional.of(user);
        } else {
            // 登录失败，增加失败计数
            int failCount = (user.getLoginFailCount() != null ? user.getLoginFailCount() : 0) + 1;
            user.setLoginFailCount(failCount);
            if (failCount >= 5) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(15));
            }
            userRepository.save(user);
            return Optional.empty();
        }
    }

    /**
     * 检查用户名是否存在
     * 
     * ----------------------------------------
     * 【方法功能】
     * ----------------------------------------
     * 
     * 用于注册时实时检查用户名是否可用
     * 
     * ----------------------------------------
     * 【为什么要单独写这个方法？】
     * ----------------------------------------
     * 
     * 1. 虽然 register() 里也检查了，但那是提交时检查
     * 2. 用户输入时实时检查体验更好
     * 3. 前端 AJAX 调用，实时显示"用户名已存在"
     * 
     * ----------------------------------------
     * 【执行流程】
     * ----------------------------------------
     * 
     * 1. 前端输入用户名
     * 2. AJAX 请求 /check-username?username=xxx
     * 3. Controller 调用此方法
     * 4. 返回 true/false
     * 5. 前端显示提示
     * 
     * @param username 用户名
     * @return true=已存在，false=不存在
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    @Transactional(readOnly = true)
    public boolean isAccountLocked(String account) {
        Optional<User> userOpt = userRepository.findByUsername(account);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhone(account);
        }
        if (userOpt.isEmpty()) return false;
        User user = userOpt.get();
        return user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now());
    }

    public String generateRememberToken(User user) {
        String token = java.util.UUID.randomUUID().toString();
        user.setRememberToken(hashToken(token));
        userRepository.save(user);
        return token;
    }

    public void clearRememberToken(User user) {
        user.setRememberToken(null);
        userRepository.save(user);
    }

    private static String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    /**
     * 修改密码（用户自行修改）
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 校验旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码不正确");
        }

        // 新旧密码不能相同
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("新密码不能与旧密码相同");
        }

        // 校验新密码强度
        String error = com.weib.security.CredentialPolicy.validatePassword(newPassword, user.getUsername(), user.getPhone());
        if (error != null) {
            throw new RuntimeException(error);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 管理员重置用户密码
     */
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        String error = com.weib.security.CredentialPolicy.validatePassword(newPassword, user.getUsername(), user.getPhone());
        if (error != null) {
            throw new RuntimeException(error);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
