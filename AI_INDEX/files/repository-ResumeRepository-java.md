# src/main/java/com/weib/repository/ResumeRepository.java

## 职责
简历数据访问，extends JpaRepository<Resume, Long>。

## 自定义查询方法 (2个)
| 方法 | 说明 |
|------|------|
| findByUserId(Long) | 按userId查(唯一) |
| existsByUserId(Long) | 用户是否已有简历 |

## 风险标记
- (无)
