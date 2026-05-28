# src/main/java/com/weib/service/JobService.java

## 职责
职位业务逻辑：列表查询/搜索(多条件组合)/CRUD/浏览量+1/已投递检查。

## 导出
- `JobService` — 职位业务服务

## 依赖
### 内部引用
- `JobRepository` — 职位数据访问(8个自定义查询方法)
- `ApplicationRepository` — 投递检查(findByJobIdAndUserId)
- `Job` — 实体
### 外部依赖
- `@Transactional`

## 关键方法
| 方法 | 事务 | 说明 |
|------|------|------|
| getAllActiveJobs() | readOnly | 查询status=active，按创建时间降序 |
| getJobById(Long) | readOnly | orElseThrow抛异常 |
| getJobsByCompanyId(Long) | readOnly | Boss查看自己的职位 |
| searchJobs(keyword,city,education) | readOnly | 三条件组合过滤(Stream filter) |
| createJob(Job) | readWrite | 新建职位 |
| updateJob(Job) | readWrite | 更新(保留原createdAt) |
| deleteJob(Long) | readWrite | 硬删除(实际项目建议软删除) |
| incrementViewCount(Long) | readWrite | 浏览量+1 |
| hasApplied(Long,Long) | readOnly | 检查是否已投递 |

## 数据流
- 搜索: keyword→findByTitleContainingIgnoreCase→city filter→education filter→返回结果
- 更新: 查询existing→保留createdAt→save

## 风险标记
- (无)
