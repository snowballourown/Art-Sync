package com.artsync.dto;

import com.artsync.domain.reservation.Reservation;
import com.artsync.domain.reservation.ReservationStatus;
import com.artsync.domain.slot.TimeSlot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 예약 응답. 예약 정보 + 해당 슬롯의 날짜/시간을 함께 담는다.
 * memberName 은 관리자 API 에서만 채워진다 (회원 자신의 목록에서는 null).
 */
public record ReservationResponse(
        Long id,
        Long slotId,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        Long memberId,
        String memberName,
        ReservationStatus status,
        LocalDateTime requestedAt,
        LocalDateTime decidedAt,
        String memo) {

    /** 회원용 — memberName 없이 생성 */
    public static ReservationResponse of(Reservation r, TimeSlot slot) {
        return new ReservationResponse(
                r.getId(),
                r.getSlotId(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                r.getMemberId(),
                null,
                r.getStatus(),
                r.getRequestedAt(),
                r.getDecidedAt(),
                r.getMemo());
    }

    /** 관리자용 — memberName 포함 */
    public static ReservationResponse of(Reservation r, TimeSlot slot, String memberName) {
        return new ReservationResponse(
                r.getId(),
                r.getSlotId(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                r.getMemberId(),
                memberName,
                r.getStatus(),
                r.getRequestedAt(),
                r.getDecidedAt(),
                r.getMemo());
    }
}
