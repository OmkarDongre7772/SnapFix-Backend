package com.snapfix.notification.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.snapfix.notification.entity.Notification;

import java.util.List;
import java.util.Optional;


@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID>{
    List<Notification> findByRecipient_Id(UUID recipient);

    List<Notification> findByRecipient_IdAndRead(UUID recipient, boolean read);


    Optional<Notification> findByNotificationIdAndRecipient_Id(UUID id, UUID recipientId);

}
