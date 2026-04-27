package com.weib.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ============================================
 * 【实体类】用户表映射
 * ============================================
 * 
 * 什么是实体类？
 * - 实体类 = Java 类 ↔ 数据库表 的映射
 * - 一个实体类对应一张表
 * - 一个实体对象对应表中的一行数据
 * 
 * 这种技术叫 ORM（Object-Relational Mapping，对象关系映射）
 * Spring Boot 使用 JPA（Java Persistence API）规范，Hibernate 是实现
 * 
 * ----------------------------------------
 * 【@Entity】JPA 实体注解
 * ----------------------------------------
 * 
 * 作用：标记这个类是一个 JPA 实体，会被映射到数据库表
 * 
 * 使用位置：类上
 * 
 * 效果：
 * - Hibernate 会自动为这个类创建表（如果 ddl-auto = update/create）
 * - 可以用 EntityManager 或 Repository 操作
 * 
 * 【常见问题】
 * Q: 实体类必须有无参构造器吗？
 * A: 是的！JPA 要求实体类必须有无参构造器（可以是 private）
 *    如果用了 Lombok @Data，它会自动生成
 * 
 * ----------------------------------------
 * 【@Table(name = "users")】表映射注解
 * ----------------------------------------
 * 
 * 作用：指定实体类对应的数据库表名
 * 
 * 属性：
 * - name：表名
 * - schema：数据库名（可选）
 * - uniqueConstraints：唯一约束
 * - indexes：索引
 * 
 * 如果不写 @Table：
 * - 默认表名 = 类名（User → user）
 * 
 * 为什么写 name = "users"？
 * - 因为 user 是 MySQL 关键字，避免冲突
 * - 习惯上表名用复数形式
 * 
 * ----------------------------------------
 * 【@Data】Lombok 注解
 * ----------------------------------------
 * 
 * 作用：自动生成常用方法，减少样板代码
 * 
 * 生成的代码：
 * - 所有字段的 getter 方法
 * - 所有非 final 字段的 setter 方法
 * - equals() 方法
 * - hashCode() 方法
 * - toString() 方法
 * 
 * 等价于：
 * @Getter + @Setter + @ToString + @EqualsAndHashCode
 * 
 * 【为什么用 Lombok？】
 * - 一个实体类有 5 个字段，手写 getter/setter 要 10 个方法
 * - 代码又臭又长，Lombok 一行搞定
 * 
 * 【IDEA 需要安装 Lombok 插件】
 * - File → Settings → Plugins → 搜索 Lombok → Install
 * - 还要开启：Settings → Build → Compiler → Annotation Processors → Enable
 */
@Entity
@Table(name = "users")
@Data
public class User {

    /**
     * ----------------------------------------
     * 【@Id】主键注解
     * ----------------------------------------
     * 
     * 作用：标记这个字段是主键
     * 
     * 使用位置：字段上
     * 
     * 要求：每个实体类必须有且只有一个 @Id 字段
     * 
     * ----------------------------------------
     * 【@GeneratedValue】主键生成策略
     * ----------------------------------------
     * 
     * 作用：指定主键的生成方式
     * 
     * 属性：
     * - strategy：生成策略
     * 
     * 策略选项：
     * 
     * 1. GenerationType.IDENTITY（最常用）
     *    - 利用数据库的自增功能（AUTO_INCREMENT）
     *    - MySQL、PostgreSQL 支持
     *    - 效率最高
     * 
     * 2. GenerationType.AUTO
     *    - JPA 自动选择策略
     *    - 不推荐，行为不确定
     * 
     * 3. GenerationType.SEQUENCE
     *    - 使用数据库序列
     *    - Oracle 默认策略
     *    - MySQL 不支持序列
     * 
     * 4. GenerationType.TABLE
     *    - 使用单独的表生成主键
     *    - 效率低，不推荐
     * 
     * 【为什么选 IDENTITY？】
     * - 我们用 MySQL，支持自增
     * - 效率最高，配置最简单
     * 
     * 【效果】
     * - 插入数据时不需要设置 id
     * - 数据库自动生成：1, 2, 3, 4, ...
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ----------------------------------------
     * 【@NotBlank】校验注解
     * ----------------------------------------
     * 
     * 作用：校验字符串不能为 null、不能为空字符串、不能全是空格
     * 
     * 来源：jakarta.validation.constraints.NotBlank
     *      （JSR-303 Bean Validation 规范）
     * 
     * 使用位置：字段上、方法参数上
     * 
     * 触发时机：
     * - Controller 接收参数时，加 @Valid 触发
     * - 手动调用 Validator.validate()
     * 
     * 【@NotBlank vs @NotNull vs @NotEmpty】
     * 
     * @NotNull：不能为 null
     *   - null → 不通过
     *   - ""   → 通过
     *   - "  " → 通过
     * 
     * @NotEmpty：不能为 null、不能为空（字符串/集合）
     *   - null → 不通过
     *   - ""   → 不通过
     *   - "  " → 通过
     * 
     * @NotBlank：不能为 null、不能为空、不能全是空格
     *   - null → 不通过
     *   - ""   → 不通过
     *   - "  " → 不通过
     *   - "a"  → 通过
     * 
     * 【message 属性】
     * - 自定义错误消息
     * - 不写则使用默认消息
     * 
     * ----------------------------------------
     * 【@Size(min, max)】长度校验
     * ----------------------------------------
     * 
     * 作用：校验字符串/集合的长度范围
     * 
     * 属性：
     * - min：最小长度（默认 0）
     * - max：最大长度（默认 Integer.MAX_VALUE）
     * - message：自定义错误消息
     * 
     * 【其他校验注解】
     * - @Min(0)       - 数值最小值
     * - @Max(100)     - 数值最大值
     * - @Email        - 邮箱格式
     * - @Pattern(regexp = "^[a-zA-Z0-9]+$") - 正则匹配
     * - @Past         - 日期必须是过去
     * - @Future       - 日期必须是未来
     * 
     * ----------------------------------------
     * 【@Column】字段映射注解
     * ----------------------------------------
     * 
     * 作用：指定字段与数据库列的映射关系
     * 
     * 属性：
     * - name：列名（默认 = 字段名）
     * - nullable：是否允许 null（默认 true）
     * - unique：是否唯一（默认 false）
     * - length：字符串长度（默认 255）
     * - columnDefinition：自定义 SQL（如 "TEXT"）
     * 
     * 【为什么写 nullable = false？】
     * - 对应数据库 NOT NULL 约束
     * - 防止脏数据
     * 
     * 【为什么写 unique = true？】
     * - 用户名不能重复
     * - 对应数据库 UNIQUE 约束
     * 
     * 【DDL 效果】
     * CREATE TABLE users (
     *   username VARCHAR(50) NOT NULL UNIQUE,
     *   ...
     * );
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度至少6位")
    @Column(nullable = false, length = 100)
    private String password;

    /**
     * LocalDateTime 是 Java 8 引入的日期时间类型
     * 比 Date 更好用：
     * - 不可变（线程安全）
     * - API 更清晰
     * - 不带时区信息
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 用户角色：seeker=求职者, boss=Boss
     */
    @Column(nullable = false, length = 20)
    private String role = "seeker";

    /**
     * 用户头像路径
     */
    @Column(length = 500)
    private String avatar;

    /**
     * 用户昵称（显示名称）
     */
    @Column(length = 50)
    private String nickname;

    /**
     * ----------------------------------------
     * 【@PrePersist】生命周期回调
     * ----------------------------------------
     * 
     * 作用：在数据插入数据库之前自动执行
     * 
     * 使用位置：方法上
     * 
     * 要求：
     * - 方法无参数
     * - 方法返回 void
     * - 可以是 private
     * 
     * 【生命周期回调注解】
     * 
     * JPA 提供的回调注解：
     * - @PrePersist  - 插入前
     * - @PostPersist - 插入后
     * - @PreUpdate   - 更新前
     * - @PostUpdate  - 更新后
     * - @PreRemove   - 删除前
     * - @PostRemove  - 删除后
     * - @PostLoad    - 查询后
     * 
     * 【这个方法的作用】
     * - 在插入数据前，自动设置创建时间和更新时间
     * - 不用手动设置，保证数据一致性
     * 
     * 【执行流程】
     * 1. userRepository.save(user)
     * 2. Hibernate 检测到 @PrePersist
     * 3. 执行 onCreate() 方法
     * 4. 执行 INSERT SQL
     * 
     * 【为什么用 protected？】
     * - 子类可以覆盖
     * - 外部不能调用（这是回调，不是业务方法）
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 更新时自动更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
