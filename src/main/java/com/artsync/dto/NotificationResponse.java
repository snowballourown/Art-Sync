package com.artsync.dto;

import com.artsync.domain.notification.Notification;
import com.artsync.domain.notification.NotificationType;

import java.time.LocalDateTime;

/** 알림 응답 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        boolean read,
        Long relatedReservationId,
        LocalDateTime createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getMessage(),
                n.isRead(),
                n.getRelatedReservationId(),
                n.getCreatedAt());
    }
}
