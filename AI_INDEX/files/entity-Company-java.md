# src/main/java/com/weib/entity/Company.java

## 职责
公司实体，映射 `companies` 表。含经纬度字段用于高德地图打点。

## 表: companies
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | PK |
| name | String(100) | 公司名称 |
| logo | String(500) | Logo路径 |
| industry | String(50) | 行业 |
| scale | String(20) | 规模(如"10000人以上") |
| description | String(2000) | 公司介绍 |
| address | String(200) | 地址 |
| latitude | Double | 纬度(高德地图) |
| longitude | Double | 经度(高德地图) |
| contactName | String(50) | 联系人 |
| contactPhone | String(20) | 联系电话 |
| contactEmail | String(100) | 联系邮箱 |
| bossId | Long | Boss用户ID |
| createdAt/updatedAt | LocalDateTime | 时间戳 |

## 生命周期
- @PrePersist → onCreate()
- @PreUpdate → onUpdate()

## 风险标记
- (无)
