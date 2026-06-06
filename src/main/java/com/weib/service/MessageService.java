package com.weib.service;

import com.weib.entity.Message;
import com.weib.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    /**
     * 保存新消息，同时更新会话的最新活动时间（用于排序）
     */
    @Transactional
    public Message saveMessage(String conversationId, Long senderId, Long receiverId,
                               String content, String messageType,
                               String fileName, String filePath, Long fileSize) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setFileName(fileName);
        message.setFilePath(filePath);
        message.setFileSize(fileSize);
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<Message> getConversationMessages(String conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    /**
     * 批量标记某会话中发给当前用户的消息为已读，并返回这些消息的发送者ID集合
     * 返回值用于向发送者推送"已读"回执
     */
    @Transactional
    public Set<Long> markAsReadAndGetSenders(String conversationId, Long receiverId) {
        List<Message> unreadMessages = messageRepository
                .findByConversationIdAndReceiverIdAndIsReadFalse(conversationId, receiverId);
        Set<Long> senders = new HashSet<>();
        for (Message msg : unreadMessages) {
            msg.setIsRead(true);
            senders.add(msg.getSenderId());
        }
        if (!unreadMessages.isEmpty()) {
            messageRepository.saveAll(unreadMessages);
        }
        return senders;
    }

    /**
     * 简单标记已读（兼容旧调用，不需要回执ID）
     */
    @Transactional
    public void markAsRead(String conversationId, Long receiverId) {
        markAsReadAndGetSenders(conversationId, receiverId);
    }

    @Transactional(readOnly = true)
    public int getUnreadCount(String conversationId, Long userId) {
        return messageRepository.countByConversationIdAndReceiverIdAndIsReadFalse(conversationId, userId);
    }

    @Transactional(readOnly = true)
    public int getTotalUnreadCount(Long userId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    /**
     * 获取会话最新一条消息，用于会话列表预览
     * 返回 Optional.empty() 表示该会话没有任何消息
     */
    @Transactional(readOnly = true)
    public Optional<Message> getLastMessage(String conversationId) {
        return messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId);
    }
}
