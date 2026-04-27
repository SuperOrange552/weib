package com.weib;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================
 * 【启动类】Spring Boot 应用入口
 * ============================================
 * 
 * 什么是启动类？
 * - Spring Boot 应用的"大门"，程序从这里开始运行
 * - main 方法是 Java 程序的标准入口
 * 
 * ----------------------------------------
 * 【@SpringBootApplication】核心注解
 * ----------------------------------------
 * 
 * 这是最重要的注解，它是三个注解的组合：
 * 
 * 1. @Configuration（配置类）
 *    - 告诉 Spring：这个类里可能定义了一些 Bean（组件）
 *    - Bean 是什么？就是 Spring 管理的对象
 *    - 例子：@Bean public DataSource dataSource() { ... }
 * 
 * 2. @EnableAutoConfiguration（自动配置）
 *    - Spring Boot 的核心功能：自动配置
 *    - 根据你引入的依赖，自动创建需要的 Bean
 *    - 例子：你引入了 spring-boot-starter-web，它就自动配置好 Tomcat、Spring MVC
 *    - 不用手写 XML 配置文件了！
 * 
 * 3. @ComponentScan（组件扫描）
 *    - 告诉 Spring：扫描当前包及子包下的所有组件
 *    - 当前包是 com.weib，所以会扫描：
 *      - com.weib.controller.*
 *      - com.weib.service.*
 *      - com.weib.repository.*
 *      - com.weib.entity.*
 *      - com.weib.config.*
 *    - 被 @Controller、@Service、@Repository、@Component 标记的类会被自动注册为 Bean
 * 
 * 【为什么要放在根包下？】
 * - 因为 @ComponentScan 默认扫描当前包及子包
 * - 如果放在 com.weib.controller 下，就扫不到 com.weib.service 了
 * 
 * 【常见问题】
 * Q: 能不能不放在根包下？
 * A: 可以，但需要手动指定扫描范围：
 *    @SpringBootApplication(scanBasePackages = "com.weib")
 */
@SpringBootApplication
public class WeibApplication {

    /**
     * 【main 方法】程序入口
     * 
     * 这是 Java 程序的标准入口方法：
     * - public：公开的，JVM 可以调用
     * - static：静态的，不需要创建对象就能调用
     * - void：没有返回值
     * - String[] args：命令行参数
     * 
     * ----------------------------------------
     * SpringApplication.run() 做了什么？
     * ----------------------------------------
     * 
     * 这一行代码背后做了非常多事情（Spring Boot 启动流程）：
     * 
     * 1. 创建 SpringApplication 对象
     *    - 判断应用类型（Servlet/Reactive）
     *    - 加载初始器（Initializer）
     *    - 加载监听器（Listener）
     * 
     * 2. 执行 run() 方法
     *    - 启动计时器
     *    - 打印 Banner（那个 Spring logo）
     *    - 创建 ApplicationContext（Spring 容器）
     *    - 准备环境（读取 application.yml）
     *    - 执行 @EnableAutoConfiguration（自动配置）
     *    - 执行 @ComponentScan（扫描组件）
     *    - 初始化所有 Bean（单例）
     *    - 启动内嵌 Tomcat
     *    - 调用 ApplicationRunner/CommandLineRunner
     *    - 打印启动成功日志
     * 
     * 【参数说明】
     * - WeibApplication.class：告诉 Spring 主配置类是哪个
     * - args：命令行参数（可以覆盖配置文件）
     * 
     * 【返回值】
     * - 返回 ConfigurableApplicationContext
     * - 可以用来获取 Bean：context.getBean(UserService.class)
     */
    public static void main(String[] args) {
        // 启动 Spring Boot 应用
        SpringApplication.run(WeibApplication.class, args);
        
        // 这行只是打印提示，不是必须的
        System.out.println("✅ 微薄项目启动成功！访问 http://localhost:8080");
    }
}
