# src/main/java/com/weib/service/MessageService.java

## 职责
聊天消息业务逻辑：保存消息、按会话ID查询(升序)、标记已读(更新isRead)、未读计数。

## 导出
- `MessageService` — 消息业务服务

## 依赖
### 内部引用
- `MessageRepository` — 消息数据访问
- `Message` — 实体
### 外部依赖
- `@Transactional`

## 关键方法
| 方法 | 事务 | 说明 |
|------|------|------|
| saveMessage(...8 params) | readWrite | 保存消息(支持text/file类型) |
| getConversationMessages(String) | readOnly | 按会话ID升序查历史 |
| markAsRead(String, Long) | readWrite | 标记receiverId的未读消息为已读 |
| getUnreadCount(String, Long) | readOnly | 未读消息计数 |

## 数据流
- 保存: 8参数→构造Message对象→save→返回(含生成ID)
- 标记已读: 查会话所有消息→过滤receiverId且isRead=false→setIsRead(true)→save
- 未读计数: Repository方法名查询 countByConversationIdAndReceiverIdAndIsReadFalse

## 风险标记
- (无)
