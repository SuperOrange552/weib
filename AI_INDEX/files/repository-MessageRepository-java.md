# src/main/java/com/weib/repository/MessageRepository.java

## 职责
聊天消息数据访问，extends JpaRepository<Message, Long>。

## 自定义查询方法 (3个)
| 方法 | 说明 |
|------|------|
| findByConversationIdOrderByCreatedAtAsc(String) | 会话历史(升序) |
| findByReceiverIdAndIsReadFalse(Long) | 某用户全部未读消息 |
| countByConversationIdAndReceiverIdAndIsReadFalse(String,Long) | 某会话的未读计数 |

## 风险标记
- (无)
