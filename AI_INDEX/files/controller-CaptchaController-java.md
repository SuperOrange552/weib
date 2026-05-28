# src/main/java/com/weib/controller/CaptchaController.java

## 职责
图形验证码生成（写入 Session）和校验（一次性消费）。

## 导出
- `CaptchaController` — 验证码控制器
- `CaptchaController.verify()` — 静态验证方法

## 依赖
### 内部引用
- `CaptchaUtil` — 验证码图片生成
### 外部依赖
- `javax.imageio.ImageIO`, `jakarta.servlet`

## 端点
| 路由 | 方法 | 说明 |
|------|------|------|
| GET /captcha | captcha() | 生成验证码图片(PNG)，存入session |

## 数据流
- 生成: CaptchaUtil.generateCode()→4位随机字符→存Session("captcha_code")→ImageIO输出PNG
- 校验: 比对输入与Session中的code→立即清除Session中的code(一次性使用)→不区分大小写

## 组件关系
- 父组件: ROOT
- 子组件: (无)
- 被调用: UserController(static verify)

## 风险标记
- (无)
