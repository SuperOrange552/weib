# src/main/java/com/weib/repository/CompanyRepository.java

## 职责
公司数据访问，extends JpaRepository<Company, Long>。

## 自定义查询方法 (5个)
| 方法 | 说明 |
|------|------|
| findByBossId(Long) | Boss的公司列表 |
| findByBossIdAndId(Long,Long) | 验证公司归属 |
| findByNameContainingIgnoreCase(String) | 模糊搜索 |
| findByIndustry(String) | 按行业查 |
| existsByBossId(Long) | Boss是否已入驻 |

## 风险标记
- (无)
