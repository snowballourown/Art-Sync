package com.artsync.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 슬롯 일괄 생성 요청.
 * 예: date=2026-05-20, startTime=10:00, endTime=18:00, slotMinutes=120
 *  → 10:00~12:00, 12:00~14:00, 14:00~16:00, 16:00~18:00 슬롯 생성
 * capacity 미지정 시 기본 4명.
 */
public record CreateSlotsRequest(
        @NotNull(message = "날짜는 필수입니다.") LocalDate date,
        @NotNull(message = "시작 시간은 필수입니다.") LocalTime startTime,
        @NotNull(message = "종료 시간은 필수입니다.") LocalTime endTime,
        @Positive(message = "슬롯 길이(분)는 1 이상이어야 합니다.") int slotMinutes,
        Integer capacity) {
}
