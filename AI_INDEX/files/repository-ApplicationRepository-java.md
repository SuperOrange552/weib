# src/main/java/com/weib/repository/ApplicationRepository.java

## 职责
投递记录数据访问，extends JpaRepository<Application, Long>。

## 自定义查询方法 (5个)
| 方法 | 说明 |
|------|------|
| findByUserId(Long) | 求职者的全部投递 |
| findByJobId(Long) | 职位的全部投递 |
| findByJobIdAndStatus(Long,String) | 职位+状态过滤 |
| findByJobIdAndUserId(Long,Long) | 去重检查(核心方法) |
| findByStatus(String) | 按状态查 |

## 关键方法
`findByJobIdAndUserId` 是投递去重的核心，被 hasApplied() 使用。

## 风险标记
- (无)
