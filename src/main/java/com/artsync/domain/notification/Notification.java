package com.artsync.domain.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 사용자에게 전달되는 앱 내 알림.
 * 설계문서 2.2 - notifications 테이블 대응.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 알림을 받는 사용자 id */
    @Column(nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 255)
    private String message;

    /** 컬럼명을 is_read 로 지정 (read 는 DB 예약어) */
    @Column(name = "is_read", nullable = false)
    private boolean read;

    /** 관련 예약 id (없을 수 있음) */
    @Column
    private Long relatedReservationId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Notification() {
        // JPA 기본 생성자
    }

    public Notification(Long recipientId, NotificationType type, String message,
                        Long relatedReservationId) {
        this.recipientId = recipientId;
        this.type = type;
        this.message = message;
        this.relatedReservationId = relatedReservationId;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.read = true;
    }

    public Long getId() {
        return id;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public Long getRelatedReservationId() {
        return relatedReservationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
