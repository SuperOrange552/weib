package com.weib.notification;
import com.weib.entity.NotificationEvent;
import org.springframework.stereotype.Component;
@Component public class NoopPushGateway implements PushGateway { public void push(NotificationEvent event) { } }
