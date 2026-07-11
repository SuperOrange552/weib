package com.weib.notification;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
@Component @RequiredArgsConstructor
public class RealtimeNotificationPublisher {
 private final SimpMessagingTemplate messaging; private final PushGateway pushGateway;
 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
 public void publish(NotificationEventCreated created){var e=created.event(); messaging.convertAndSendToUser(e.getRecipientId().toString(),"/queue/notifications",e); pushGateway.push(e);}
}
