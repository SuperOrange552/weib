package com.weib.repository;

import com.weib.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ============================================
 * 【Repository 接口】数据访问层
 * ============================================
 * 
 * 什么是 Repository？
 * - Repository = 仓库，负责数据的增删改查
 * - 是 DAO（Data Access Object）模式的实现
 * - Spring Data JPA 让我们只需要写接口，不需要写实现！
 * 
 * ----------------------------------------
 * 【@Repository】Spring 组件注解
 * ----------------------------------------
 * 
 * 作用：标记这个接口/类是数据访问组件
 * 
 * 特点：
 * - 是 @Component 的衍生注解
 * - 被 @ComponentScan 自动扫描并注册为 Bean
 * - 支持持久层异常转换（把原生异常转为 Spring 异常）
 * 
 * 【三层架构注解对比】
 * - @Controller - 控制层（处理请求）
 * - @Service    - 业务层（业务逻辑）
 * - @Repository - 数据层（数据访问）
 * 
 * 它们本质上都是 @Component，只是语义不同
 * 
 * 【为什么这里可以不写 @Repository？】
 * - JpaRepository 的实现类会自动被 Spring 注册
 * - 但写上更清晰，也方便 IDE 检查
 * 
 * ----------------------------------------
 * 【继承 JpaRepository】魔法所在
 * ----------------------------------------
 * 
 * JpaRepository<实体类型, 主键类型>
 * 
 * 继承后自动获得的方法（不需要写实现！）：
 * 
 * 【保存/更新】
 * - save(S entity)           - 保存或更新（id 存在则更新）
 * - saveAll(Iterable<S>)     - 批量保存
 * 
 * 【查询】
 * - findById(ID id)          - 按 ID 查询，返回 Optional
 * - findAll()                - 查询全部
 * - findAllById(Iterable<ID>)- 按 ID 批量查询
 * - count()                  - 统计数量
 * - existsById(ID id)        - 判断是否存在
 * 
 * 【删除】
 * - deleteById(ID id)        - 按 ID 删除
 * - delete(T entity)         - 删除实体
 * - deleteAll()              - 删除全部
 * - deleteAllInBatch()       - 批量删除（一条 SQL）
 * 
 * 【分页/排序】
 * - findAll(Pageable)        - 分页查询
 * - findAll(Sort)            - 排序查询
 * 
 * 【原理 - 动态代理】
 * - Spring 在启动时，会为每个 Repository 接口生成实现类
 * - 使用 JDK 动态代理或 CGLIB
 * - 方法调用时，代理对象解析方法名，生成 SQL，执行查询
 * 
 * ----------------------------------------
 * 【自定义查询方法】方法名即查询
 * ----------------------------------------
 * 
 * Spring Data JPA 会根据方法名自动生成 SQL！
 * 
 * 规则：
 * - find...By...  - 查询
 * - count...By... - 统计
 * - delete...By...- 删除
 * - exists...By...- 判断存在
 * 
 * 【关键字】
 * - And          - WHERE a = ? AND b = ?
 * - Or           - WHERE a = ? OR b = ?
 * - Between      - WHERE a BETWEEN ? AND ?
 * - LessThan     - WHERE a < ?
 * - GreaterThan  - WHERE a > ?
 * - Like         - WHERE a LIKE ?
 * - Containing   - WHERE a LIKE %?%（自动加百分号）
 * - In           - WHERE a IN (?)
 * - OrderBy...Asc/Desc - ORDER BY ... ASC/DESC
 * 
 * 【例子】
 * - findByName(String name)
 *   → SELECT * FROM user WHERE name = ?
 * 
 * - findByNameAndAge(String name, Integer age)
 *   → SELECT * FROM user WHERE name = ? AND age = ?
 * 
 * - findByAgeGreaterThan(Integer age)
 *   → SELECT * FROM user WHERE age > ?
 * 
 * - findByNameContaining(String keyword)
 *   → SELECT * FROM user WHERE name LIKE %?%
 * 
 * - findByAgeBetween(Integer min, Integer max)
 *   → SELECT * FROM user WHERE age BETWEEN ? AND ?
 * 
 * - countByStatus(Integer status)
 *   → SELECT COUNT(*) FROM user WHERE status = ?
 * 
 * - existsByName(String name)
 *   → SELECT EXISTS(SELECT 1 FROM user WHERE name = ?)
 * 
 * 【返回类型】
 * - 实体类型           - 查询一个
 * - List<实体>        - 查询多个
 * - Optional<实体>    - 查询一个，可能为空（推荐）
 * - Page<实体>        - 分页结果
 * - Long/Integer      - 统计数量
 * - boolean           - 判断存在
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查询用户
     * 
     * 方法名解析：
     * - findBy   - 查询操作
     * - Username - 字段名 username
     * 
     * 生成的 SQL：
     * SELECT * FROM users WHERE username = ?
     * 
     * 【为什么返回 Optional？】
     * - 用户可能存在，也可能不存在
     * - Optional 强制调用者处理"不存在"的情况
     * - 避免 NullPointerException
     * 
     * 【使用方式】
     * // 方式1：判断存在并获取
     * Optional<User> opt = userRepository.findByUsername("admin");
     * if (opt.isPresent()) {
     *     User user = opt.get();
     * }
     * 
     * // 方式2：orElse（推荐）
     * User user = opt.orElse(null);
     * 
     * // 方式3：orElseThrow
     * User user = opt.orElseThrow(() -> new RuntimeException("用户不存在"));
     * 
     * // 方式4：ifPresent
     * opt.ifPresent(user -> System.out.println(user.getUsername()));
     * 
     * // 方式5：filter + map（函数式）
     * opt.filter(u -> u.getPassword().equals(password))
     *    .map(User::getUsername)
     *    .orElse("unknown");
     * 
     * @param username 用户名
     * @return Optional 包装的用户对象
     */
    Optional<User> findByUsername(String username);

    /**
     * 检查用户名是否存在
     * 
     * 方法名解析：
     * - exists  - 判断存在
     * - By      - 条件
     * - Username- 字段名
     * 
     * 生成的 SQL：
     * SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)
     * 
     * 或者：
     * SELECT COUNT(*) FROM users WHERE username = ? LIMIT 1
     * 
     * 【为什么用 exists 而不是 find？】
     * - exists 只需要判断存在，不需要查询所有字段
     * - 效率更高
     * - 语义更清晰
     * 
     * @param username 用户名
     * @return true=存在，false=不存在
     */
    boolean existsByUsername(String username);

    Optional<User> findByRememberToken(String rememberToken);
}
