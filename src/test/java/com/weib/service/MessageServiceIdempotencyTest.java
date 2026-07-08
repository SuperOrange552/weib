package com.weib.service;
import com.weib.entity.Message;import com.weib.repository.MessageRepository;import org.junit.jupiter.api.Test;import java.util.Optional;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
class MessageServiceIdempotencyTest {
 @Test void duplicateClientMessageReturnsExistingWithoutInsert(){var repo=mock(MessageRepository.class);var existing=new Message();existing.setId(9L);when(repo.findBySenderIdAndClientMessageId(1L,"client-1234")).thenReturn(Optional.of(existing));var service=new MessageService(repo);var result=service.saveMessage("c",1L,2L,"hi","text",null,null,null,"client-1234");assertSame(existing,result);verify(repo,never()).save(any());}
 @Test void unsafeClientMessageIdIsRejected(){var service=new MessageService(mock(MessageRepository.class));assertThrows(IllegalArgumentException.class,()->service.saveMessage("c",1L,2L,"hi","text",null,null,null,"bad key"));}
}