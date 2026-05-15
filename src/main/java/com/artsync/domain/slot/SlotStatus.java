package com.artsync.domain.slot;

/**
 * 시간 슬롯의 상태 (조회 편의를 위한 캐시성 값).
 * 마감 여부는 항상 서버에서 날짜/시각으로 재검증한다. (설계문서 6.1)
 *
 * AVAILABLE - 예약 가능
 * FULL      - 정원 참
 * CLOSED    - 마감 (예약일 전날 23:59:59 경과)
 */
public enum SlotStatus {
    AVAILABLE,
    FULL,
    CLOSED
}
