package com.snapfix.notification.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.snapfix.auth.security.CustomUserDetails;
import com.snapfix.notification.dto.NotificationResponse;
import com.snapfix.notification.entity.Notification;
import com.snapfix.notification.entity.NotificationType;
import com.snapfix.notification.repository.NotificationRepository;
import com.snapfix.user.entity.User;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<NotificationResponse> getNotifications() {
        UUID recipient = getCurrentUserId();
        List<Notification> notifications = notificationRepository.findByRecipient_Id(recipient);
        return mapToResponse(notifications);
    }

    public List<NotificationResponse> getNotifications(Boolean read) {
        UUID recipient = getCurrentUserId();
        List<Notification> notifications;
        if (read != null) {
            notifications = notificationRepository.findByRecipient_IdAndRead(recipient, read);
        } else {
            notifications = notificationRepository.findByRecipient_Id(recipient);
        }
        return mapToResponse(notifications);
    }

    public void markRead(UUID id) {
        UUID currentUserId = getCurrentUserId();
        Notification notification = notificationRepository.findByNotificationIdAndRecipient_Id(id, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void createNotification(User recipient, NotificationType type, String message) {
        if (recipient == null) {
            throw new IllegalArgumentException("Notification recipient is required");
        }
        notificationRepository.save(new Notification(recipient, type, message));
    }

    //
    // Utilities
    //

    private List<NotificationResponse> mapToResponse(List<Notification> notifications) {
        return notifications.stream()
                .map(NotificationResponse::new)
                .toList();
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new IllegalStateException("Invalid authentication context");
        }

        return user.getId();
    }

}
