package com.artsync.dto;

import com.artsync.domain.reservation.Reservation;
import com.artsync.domain.reservation.ReservationStatus;
import com.artsync.domain.slot.TimeSlot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 예약 응답. 예약 정보 + 해당 슬롯의 날짜/시간을 함께 담는다.
 */
public record ReservationResponse(
        Long id,
        Long slotId,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        Long memberId,
        ReservationStatus status,
        LocalDateTime requestedAt,
        LocalDateTime decidedAt,
        String memo) {

    public static ReservationResponse of(Reservation r, TimeSlot slot) {
        return new ReservationResponse(
                r.getId(),
                r.getSlotId(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                r.getMemberId(),
                r.getStatus(),
                r.getRequestedAt(),
                r.getDecidedAt(),
                r.getMemo());
    }
}
