package com.weib.notification;

import com.weib.entity.NotificationEvent;
import com.weib.repository.NotificationEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationEventServiceTest {
    @Test
    void eventIdIsIdempotentAndRecipientRoleIsPersisted() {
        NotificationEventRepository repository = mock(NotificationEventRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        when(repository.findByEventId("evt-1")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        NotificationEventService service = new NotificationEventService(repository, publisher);

        NotificationEvent event = service.create("evt-1", 7L, "SEEKER", "APPLICATION_VIEWED", 22L, "简历已查看", "{}");

        assertEquals("SEEKER", event.getRecipientRole());
        verify(repository).save(any(NotificationEvent.class));
        verify(publisher).publishEvent(any(NotificationEventCreated.class));
    }
}
