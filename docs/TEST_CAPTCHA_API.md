# API 自动化测试验证码接口

## 用途

`GET /api/test/captcha` 仅用于受控的接口自动化测试。它会直接返回验证码明文，并将同一个验证码写入当前 `JSESSIONID` 会话，Python 脚本无需识别验证码图片。

该接口默认关闭，不影响正常的 `/captcha` 图片验证码流程。

## 开启与关闭

临时开启：

```bash
TEST_CAPTCHA_API_ENABLED=true
TEST_CAPTCHA_ACCESS_KEY=<至少16位的随机密钥>
```

关闭：

```bash
TEST_CAPTCHA_API_ENABLED=false
```

修改服务器环境变量后重启 `weib.service`。关闭后 Controller 不会注册，访问路径返回 HTTP 404。

## 请求

```http
GET https://superorange.top/api/test/captcha
X-Test-Access-Key: <测试密钥>
```

成功响应：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "captcha": "AB12",
    "expiresInSeconds": 120
  }
}
```

测试开关关闭、密钥缺失或密钥错误时统一返回 HTTP 404。刷新过快时返回 HTTP 429 和 `Retry-After`。

## Python requests 登录示例

```python
import os
import requests

base_url = "https://superorange.top"
test_key = os.environ["TEST_CAPTCHA_ACCESS_KEY"]

session = requests.Session()

captcha_response = session.get(
    f"{base_url}/api/test/captcha",
    headers={"X-Test-Access-Key": test_key},
    timeout=10,
)
captcha_response.raise_for_status()
captcha = captcha_response.json()["data"]["captcha"]

login_response = session.post(
    f"{base_url}/api/mobile/auth/login",
    json={
        "username": "seeker_ahua",
        "password": "Weib@123456",
        "captcha": captcha,
        "selectedRole": "SEEKER",
    },
    timeout=10,
)
login_response.raise_for_status()
access_token = login_response.json()["data"]["accessToken"]

session.headers["Authorization"] = f"Bearer {access_token}"
print(session.get(f"{base_url}/api/mobile/auth/me", timeout=10).json())
```

测试验证码请求和登录请求必须使用同一个 `requests.Session()`。访问密钥只保存在测试环境变量中，禁止提交到 Git、接口测试报告或公开集合。
