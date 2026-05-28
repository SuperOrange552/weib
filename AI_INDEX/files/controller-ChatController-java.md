# src/main/java/com/weib/controller/ChatController.java

## 职责
Boss与求职者实时聊天：WebSocket消息处理(STOMP)、PDF文件上传/下载、聊天页面。

## 导出
- `ChatController` — 聊天控制器
- `ChatController.Result<T>` — 聊天模块专用统一返回格式

## 依赖
### 内部引用
- `MessageService` — 消息存储/会话查询/标记已读
- `ApplicationService` — 投递记录查询
- `JobService` — 职位查询
- `CompanyService` — 公司查询(获取BossId)
- `SimpMessagingTemplate` — STOMP 消息发送
### 外部依赖
- Spring MVC, Spring WebSocket, `java.nio.file`

## 端点
| 路由 | 方法 | 说明 |
|------|------|------|
| GET /chat/{applicationId} | chatPage() | 聊天页面(含身份验证) |
| POST /chat/upload | uploadFile() | 上传PDF(@ResponseBody) |
| GET /chat/file/{storedName} | downloadFile() | 下载PDF附件 |
| WebSocket /app/chat | handleMessage() | STOMP消息处理 |

## 数据流
- 聊天页面: applicationId→验证身份(投递者或Boss)→加载历史消息→标记已读→渲染chat.html
- WebSocket: 客户端→STOMP /app/chat→保存消息→convertAndSendToUser发送给接收方
- 文件: 上传PDF到${storage.chat-dir}→下载时校验登录

## 安全要点
- 聊天页面验证：用户必须是投递者或职位所属公司的Boss
- 文件下载验证登录状态
- 仅允许PDF上传

## 组件关系
- 父组件: ROOT
- 子组件: (无)

## 风险标记
- `many-exports`: 含内部 Result 类
