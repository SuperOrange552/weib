package com.weib.service;

import com.weib.entity.Message;
import com.weib.repository.MessageRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.List;

class MessageIdentityIsolationTest {
    @Test
    void savedMessagePersistsBothBusinessIdentities() {
        MessageRepository repository = mock(MessageRepository.class);
        when(repository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MessageService service = new MessageService(repository);

        Message message = service.saveMessage("app_1", 7L, "SEEKER", 8L, "BOSS",
                "hello", "text", null, null, null, "client-12345");

        assertEquals("SEEKER", message.getSenderRole());
        assertEquals("BOSS", message.getReceiverRole());
        verify(repository).findBySenderIdAndSenderRoleAndClientMessageId(7L, "SEEKER", "client-12345");

        when(repository.findVisibleToIdentity("app_1", 7L, "SEEKER")).thenReturn(List.of(message));
        assertEquals(1, service.getConversationMessages("app_1", 7L, "SEEKER").size());
    }
}
