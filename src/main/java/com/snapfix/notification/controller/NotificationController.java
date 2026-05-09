package com.snapfix.notification.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.snapfix.notification.dto.NotificationResponse;
import com.snapfix.notification.service.NotificationService;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;



@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService){
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CITIZEN', 'WORKER', 'ADMIN')")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
        @RequestParam(required = false) Boolean unread
    ) {
        Boolean read = unread == null ? null : !unread;
        return ResponseEntity.ok(notificationService.getNotifications(read));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('CITIZEN', 'WORKER', 'ADMIN')")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        notificationService.markRead(id);
        return ResponseEntity.noContent().build();
    }

    
    
}
