# Spring & Spring Boot 注解速查表

## 📌 核心注解

| 注解 | 作用 | 使用位置 |
|------|------|---------|
| `@SpringBootApplication` | 启动类组合注解 | 启动类 |
| `@Configuration` | 配置类 | 配置类 |
| `@Component` | 通用组件 | 任意类 |
| `@Bean` | 定义 Bean | 配置方法 |

---

## 🏗️ 分层注解

| 注解 | 层级 | 说明 |
|------|------|------|
| `@Controller` | 控制层 | 返回视图页面 |
| `@RestController` | 控制层 | 返回 JSON 数据 |
| `@Service` | 业务层 | 业务逻辑 |
| `@Repository` | 数据层 | 数据访问 |

---

## 🌐 Web 注解

| 注解 | 说明 |
|------|------|
| `@RequestMapping` | 通用请求映射 |
| `@GetMapping` | GET 请求 |
| `@PostMapping` | POST 请求 |
| `@PutMapping` | PUT 请求 |
| `@DeleteMapping` | DELETE 请求 |
| `@PathVariable` | 路径变量 `/user/{id}` |
| `@RequestParam` | 请求参数 `?name=xxx` |
| `@RequestBody` | 请求体（JSON） |
| `@ResponseBody` | 返回 JSON |

---

## 💾 JPA 注解

| 注解 | 说明 |
|------|------|
| `@Entity` | 实体类 |
| `@Table` | 表映射 |
| `@Id` | 主键 |
| `@GeneratedValue` | 主键生成策略 |
| `@Column` | 字段映射 |
| `@OneToMany` | 一对多 |
| `@ManyToOne` | 多对一 |
| `@ManyToMany` | 多对多 |

---

## ✅ 校验注解

| 注解 | 说明 |
|------|------|
| `@NotNull` | 不能为 null |
| `@NotBlank` | 非空字符串 |
| `@NotEmpty` | 集合/数组非空 |
| `@Size(min, max)` | 长度范围 |
| `@Min` / `@Max` | 数值范围 |
| `@Email` | 邮箱格式 |
| `@Pattern` | 正则匹配 |

---

## 🔧 Lombok 注解

| 注解 | 说明 |
|------|------|
| `@Data` | getter + setter + toString + equals + hashCode |
| `@Getter` / `@Setter` | 单独生成 |
| `@RequiredArgsConstructor` | 构造器注入 |
| `@Builder` | 建造者模式 |
| `@Slf4j` | 日志 |

---

## 🔒 生命周期回调

| 注解 | 触发时机 |
|------|---------|
| `@PostConstruct` | Bean 初始化后 |
| `@PreDestroy` | Bean 销毁前 |
| `@PrePersist` | 插入数据库前 |
| `@PostPersist` | 插入数据库后 |
| `@PreUpdate` | 更新数据库前 |
| `@PostUpdate` | 更新数据库后 |
