# src/main/java/com/weib/config/WebSocketConfig.java

## 职责
WebSocket STOMP 配置：消息代理(点对点+广播)、STOMP端点注册(带握手认证)、SockJS回退。

## 导出
- `WebSocketConfig` — implements WebSocketMessageBrokerConfigurer

## 配置要点
- 消息代理: `/topic`(广播), `/queue`(点对点)
- 应用前缀: `/app` (客户端发送到 /app/chat)
- 用户前缀: `/user` (服务端推送到特定用户)
- STOMP端点: `/ws` (允许所有来源,SockJS)
- 握手拦截: 验证Session中的User→注入userId到WebSocket attributes

## 安全要点
- 握手前检查 `session.getAttribute("user")` → 未登录拒绝WebSocket连接
- 将userId注入attributes供ChatController使用

## 风险标记
- (无)
