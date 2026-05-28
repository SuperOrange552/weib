# src/main/java/com/weib/entity/Message.java

## 职责
聊天消息实体，映射 `messages` 表。支持文本和文件两种消息类型。

## 表: messages
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | PK |
| conversationId | String(64) | 会话ID (格式: "app_{applicationId}") |
| senderId | Long | 发送者ID |
| receiverId | Long | 接收者ID |
| content | String(2000) | 消息内容 |
| messageType | String(20) | "text"/"file"，默认text |
| fileName | String(500) | 文件名(file类型) |
| filePath | String(500) | 文件路径(file类型) |
| fileSize | Long | 文件大小(file类型) |
| isRead | Boolean | 是否已读，默认false |
| createdAt | LocalDateTime | @PrePersist自动设置 |

## 风险标记
- (无)
