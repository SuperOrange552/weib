# 微招 Android App

原生 Kotlin + Jetpack Compose 客户端，只包含求职者与招聘者（Boss）两种身份。管理员继续使用 Web 管理后台，App 不包含管理员入口和 `/api/admin/**` 调用。

## 环境

| Variant | API 地址 | 用途 |
|---|---|---|
| `prodDebug` | `http://superorange.top/` | 公网服务器 |
| `localDebug` | `https://10.0.2.2:8443/` | Android 模拟器连接本机 HTTPS |

公网仅对 `superorange.top` 放行明文 HTTP；本地自签名证书的宽松校验仅在 debug + local flavor 启用。

## 构建

```powershell
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat testProdDebugUnitTest assembleProdDebug
```

APK：`app/build/outputs/apk/prod/debug/app-prod-debug.apk`

## UI 规范

- 品牌主色 `#0F766E`，强调色 `#00A896`。
- 珠光白背景 `#F5F7FB`、白色卡片、14dp 圆角。
- 标题 `#0F172A`、正文 `#334155`、辅助文字 `#64748B`。
- 除青色按钮内文字外，不在浅色背景使用白色正文。
- 求职者和 Boss 使用独立底部导航，所有图标、标题栏和内容边距保持一致。

## 登录与验证码

- 验证码通过同一个 OkHttp CookieJar 保持 `JSESSIONID`，有效期显示为 2 分钟倒计时。
- 未填写账号和密码时禁止手动刷新验证码。
- 登录成功后保存 JWT，后续请求使用 `Authorization: Bearer <token>`。
- 管理员账号会返回“App仅支持求职者和招聘者登录”。
