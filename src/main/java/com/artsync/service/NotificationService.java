package com.artsync.service;

import com.artsync.common.exception.NotFoundException;
import com.artsync.domain.notification.Notification;
import com.artsync.domain.notification.NotificationRepository;
import com.artsync.domain.notification.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 앱 내 알림 생성/조회 비즈니스 로직.
 * 대응 요구사항: FR-06, FR-08 / 설계문서 6.3
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * 알림 생성. 다른 서비스(ReservationService)에서 호출한다.
     */
    @Transactional
    public Long create(Long recipientId, NotificationType type, String message,
                       Long relatedReservationId) {
        Notification notification =
                new Notification(recipientId, type, message, relatedReservationId);
        return notificationRepository.save(notification).getId();
    }

    /** 내 알림 목록 (안 읽은 것 우선, 그다음 최신순) */
    public List<Notification> getMyNotifications(Long userId) {
        return notificationRepository.findByRecipientIdOrderByReadAscCreatedAtDesc(userId);
    }

    /** 안 읽은 알림 개수 (뱃지용) */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    /** 알림 읽음 처리 */
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("알림을 찾을 수 없습니다. id=" + notificationId));
        notification.markAsRead();
    }
}
