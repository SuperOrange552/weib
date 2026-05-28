# src/main/java/com/weib/repository/JobRepository.java

## 职责
职位数据访问，extends JpaRepository<Job, Long>。

## 自定义查询方法 (8个)
| 方法 | 说明 |
|------|------|
| findByCompanyId(Long) | 按公司ID查 |
| findByStatus(String) | 按状态查(active/closed) |
| findByCompanyIdAndStatus(Long,String) | 公司+状态组合 |
| findByTitleContainingIgnoreCase(String) | 标题模糊搜索(忽略大小写) |
| findByCity(String) | 按城市查 |
| findByEducation(String) | 按学历查 |
| findByStatusOrderByCreatedAtDesc(String) | 活跃职位(最新在前) |
| findByCompanyIdOrderByCreatedAtDesc(Long) | 公司职位(最新在前) |

## 风险标记
- (无)
