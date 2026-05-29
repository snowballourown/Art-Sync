package com.artsync.dto;

import com.artsync.domain.slot.SlotStatus;
import com.artsync.domain.slot.TimeSlot;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 시간 슬롯 응답.
 * confirmedCount(확정 인원), remaining(남은 자리)을 함께 내려준다.
 */
public record SlotResponse(
        Long id,
        Long spaceId,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        int capacity,
        boolean active,
        SlotStatus status,
        long confirmedCount,
        long remaining) {

    public static SlotResponse of(TimeSlot slot, long confirmedCount) {
        long remaining = Math.max(0, slot.getCapacity() - confirmedCount);
        return new SlotResponse(
                slot.getId(),
                slot.getSpaceId(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getCapacity(),
                slot.isActive(),
                slot.getStatus(),
                confirmedCount,
                remaining);
    }
}
