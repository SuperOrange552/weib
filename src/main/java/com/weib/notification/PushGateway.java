package com.weib.notification;
import com.weib.entity.NotificationEvent;
public interface PushGateway { void push(NotificationEvent event); }
