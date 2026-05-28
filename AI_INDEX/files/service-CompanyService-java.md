# src/main/java/com/weib/service/CompanyService.java

## 职责
公司业务逻辑：按ID/BossId查询、创建、更新(保留createdAt)、存在检查、模糊搜索。

## 导出
- `CompanyService` — 公司业务服务

## 依赖
### 内部引用
- `CompanyRepository` — 公司数据访问
- `Company` — 实体
### 外部依赖
- `@Transactional`

## 关键方法
| 方法 | 事务 | 说明 |
|------|------|------|
| getCompanyById(Long) | readOnly | orElseThrow |
| getCompanyByBossId(Long) | readOnly | stream.findFirst() |
| createCompany(Company) | readWrite | 新建公司 |
| updateCompany(Company) | readWrite | 保留原createdAt |
| existsByBossId(Long) | readOnly | Boss是否已入驻 |
| searchCompanies(String) | readOnly | 名称模糊匹配 |

## 风险标记
- (无)
