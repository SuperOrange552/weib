# src/main/java/com/weib/service/ResumeService.java

## 职责
简历业务逻辑：按ID/UserId查询、保存(新建默认draft状态)、存在检查。

## 导出
- `ResumeService` — 简历业务服务

## 依赖
### 内部引用
- `ResumeRepository` — 简历数据访问
- `Resume` — 实体
### 外部依赖
- `@Transactional`

## 关键方法
| 方法 | 事务 | 说明 |
|------|------|------|
| getResumeById(Long) | readOnly | orElseThrow |
| getResumeByUserId(Long) | readOnly | 按userId查 |
| saveResume(Resume) | readWrite | 新建设status="draft"→save |
| saveOrUpdateResume(Resume) | readWrite | 直接save(与saveResume功能重复) |
| existsByUserId(Long) | readOnly | 检查是否有简历 |

## 注意
`saveResume()` 和 `saveOrUpdateResume()` 功能重复，后者为兼容性保留。

## 风险标记
- (无)
