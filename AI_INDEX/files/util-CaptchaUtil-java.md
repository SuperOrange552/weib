# src/main/java/com/weib/util/CaptchaUtil.java

## 职责
图形验证码工具：4位随机字符生成 + BufferedImage 图片绘制(含噪点、干扰线、旋转字符)

## 导出
- `CaptchaUtil.generateCode()` — 随机4位大写字母+数字
- `CaptchaUtil.generateImage(String)` — 生成120×44 PNG图片(深色背景+青色干扰)

## 配置
- 尺寸: 120×44px
- 字符池: "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" (排除易混淆字符 I/O/0/1)
- 背景色: 深蓝黑(20,20,30)
- 干扰: 60个噪点 + 4条干扰线 + 字体/颜色/角度随机

## 依赖
- `java.awt.*` — AWT图形绘制(无外部依赖)

## 风险标记
- (无)
