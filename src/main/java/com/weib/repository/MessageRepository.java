package com.weib.repository;

import com.weib.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    @Query("select m from Message m where m.conversationId=:conversationId and "
            + "((m.senderId=:userId and m.senderRole=:role) or (m.receiverId=:userId and m.receiverRole=:role)) "
            + "order by m.createdAt asc")
    List<Message> findVisibleToIdentity(@Param("conversationId") String conversationId,
                                        @Param("userId") Long userId, @Param("role") String role);

    List<Message> findByReceiverIdAndIsReadFalse(Long receiverId);

    int countByConversationIdAndReceiverIdAndIsReadFalse(String conversationId, Long receiverId);

    int countByReceiverIdAndIsReadFalse(Long receiverId);

    Optional<Message> findByFilePath(String filePath);

    Optional<Message> findBySenderIdAndClientMessageId(Long senderId, String clientMessageId);

    Optional<Message> findBySenderIdAndSenderRoleAndClientMessageId(Long senderId, String senderRole, String clientMessageId);

    /**
     * 查某会话最新一条消息，用于会话列表预览
     */
    Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(String conversationId);

    /**
     * 查某会话中某人发的所有未读消息，用于标记已读后发送回执
     */
    List<Message> findByConversationIdAndSenderIdAndIsReadFalse(String conversationId, Long senderId);

    /**
     * 查某会话中发给某人的所有未读消息，用于批量标记已读
     */
    List<Message> findByConversationIdAndReceiverIdAndIsReadFalse(String conversationId, Long receiverId);

    List<Message> findByConversationIdAndReceiverIdAndReceiverRoleAndIsReadFalse(
            String conversationId, Long receiverId, String receiverRole);

    int countByConversationIdAndReceiverIdAndReceiverRoleAndIsReadFalse(
            String conversationId, Long receiverId, String receiverRole);

    int countByReceiverIdAndReceiverRoleAndIsReadFalse(Long receiverId, String receiverRole);
}
