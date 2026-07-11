package com.weib.notification;

import com.weib.entity.NotificationEvent;
import com.weib.repository.NotificationEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List; import java.util.Locale;

@Service @RequiredArgsConstructor
public class NotificationEventService {
    private final NotificationEventRepository repository;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public NotificationEvent create(String eventId, Long recipientId, String recipientRole, String eventType,
                                    Long relatedId, String title, String payload) {
        return repository.findByEventId(eventId).orElseGet(() -> {
            NotificationEvent event=new NotificationEvent(); event.setEventId(eventId); event.setRecipientId(recipientId);
            event.setRecipientRole(recipientRole.toUpperCase(Locale.ROOT)); event.setEventType(eventType);
            event.setRelatedId(relatedId); event.setTitle(title); event.setPayload(payload);
            NotificationEvent saved=repository.save(event); publisher.publishEvent(new NotificationEventCreated(saved)); return saved;
        });
    }

    @Transactional(readOnly=true)
    public List<NotificationEvent> after(Long userId,String role,long afterId,int limit){
        return repository.findByRecipientIdAndRecipientRoleAndIdGreaterThanOrderByIdAsc(userId,role.toUpperCase(Locale.ROOT),afterId,PageRequest.of(0,Math.min(Math.max(limit,1),100)));
    }

    @Transactional
    public void markRead(Long userId,String role,Long id){ repository.findByIdAndRecipientIdAndRecipientRole(id,userId,role.toUpperCase(Locale.ROOT)).ifPresent(e->{e.setIsRead(true);repository.save(e);}); }
}
