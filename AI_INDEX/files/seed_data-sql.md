# seed_data.sql

## 职责
测试种子数据脚本，可直接导入MySQL。

## 数据规模
| 表 | 条数 | 说明 |
|-----|------|------|
| users | 13 | 5 Boss + 8 求职者，密码均为 123456(BCrypt) |
| companies | 5 | 字节跳动/华为/阿里巴巴/微众科技/星云互娱(含经纬度) |
| jobs | 50 | 每公司10条，覆盖技术/产品/运营/设计等岗位 |
| resumes | 15 | 含完整的学历/工作/项目经历 |
| applications | 25 | 覆盖 pending/interview/accepted/rejected 各状态 |

## 密码
所有用户密码: `123456`
BCrypt hash: `$2b$10$sRQ61wTcCk1TeAGSlXlAbud3W0JMyoTRsuDPeeEEnMp79m5dXfKHe`

## 风险标记
- `large-file`: 207行
