package com.snapfix.notification.dto;

import java.time.Instant;
import java.util.UUID;

import com.snapfix.notification.entity.Notification;
import com.snapfix.notification.entity.NotificationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationResponse {
    
    private UUID notificationId;
    
    private UUID recipient;

    private NotificationType type;

    private String message;

    private Boolean read;

    private Instant createdAt;

    public NotificationResponse(Notification notification) {
        this.notificationId = notification.getNotificationId();
        this.type = notification.getType();
        this.recipient = notification.getRecipient().getId();
        this.message = notification.getMessage();
        this.createdAt = notification.getCreatedAt();
        this.read = notification.isRead();
    }

    

}
