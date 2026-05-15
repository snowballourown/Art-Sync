package com.artsync.domain.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Notification 엔터티 영속성 처리.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 내 알림 목록 (안 읽은 것 우선, 그다음 최신순) */
    List<Notification> findByRecipientIdOrderByReadAscCreatedAtDesc(Long recipientId);

    /** 안 읽은 알림 개수 (뱃지용) */
    long countByRecipientIdAndReadFalse(Long recipientId);
}
