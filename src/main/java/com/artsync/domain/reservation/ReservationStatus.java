package com.artsync.domain.reservation;

/**
 * 예약 상태 (설계문서 3.3 상태 흐름).
 *
 * REQUESTED - 요청중 (회원이 요청, 사장님 처리 대기)
 * CONFIRMED - 확정 (사장님이 최종 수락)
 * REJECTED  - 거절 (사장님이 거절)
 * CANCELLED - 취소 (마감 전 취소, 선택 기능)
 */
public enum ReservationStatus {
    REQUESTED,
    CONFIRMED,
    REJECTED,
    CANCELLED
}
