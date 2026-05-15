package com.artsync.domain.notification;

/**
 * 알림 종류 (설계문서 6.3).
 *
 * RESERVATION_REQUESTED - 회원이 예약 요청 → 사장님에게
 * RESERVATION_CONFIRMED - 사장님이 수락    → 회원에게
 * RESERVATION_REJECTED  - 사장님이 거절    → 회원에게
 * RESERVATION_CANCELLED - 예약 취소        → 상대방에게
 */
public enum NotificationType {
    RESERVATION_REQUESTED,
    RESERVATION_CONFIRMED,
    RESERVATION_REJECTED,
    RESERVATION_CANCELLED
}
