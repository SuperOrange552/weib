# src/main/java/com/weib/WeibApplication.java

## 职责
Spring Boot 应用启动入口，@SpringBootApplication 主类，自动触发组件扫描和自动配置。

## 导出
- `WeibApplication` — 主类，含 `main()` 方法启动嵌入式 Tomcat

## 依赖
### 内部引用
- (无)
### 外部依赖
- `org.springframework.boot`

## API 调用
- (无)

## 状态管理
- (无)

## 数据流
- 数据来源: 无
- 数据传递: JVM 启动 → SpringApplication.run() → 扫描 com.weib 包下所有组件 → 初始化 IoC 容器 → 启动 Tomcat
- 副作用: 启动内嵌 Tomcat，监听 8443(HTTPS) + 8080(HTTP→重定向)

## 组件关系
- 父组件: ROOT (JVM 入口)
- 子组件: 无(通过 @ComponentScan 隐式管理所有子组件)

## 风险标记
- (无)
