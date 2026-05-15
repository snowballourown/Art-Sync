package com.artsync.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/** 슬롯 시간 수정 요청 */
public record UpdateSlotTimeRequest(
        @NotNull(message = "시작 시간은 필수입니다.") LocalTime startTime,
        @NotNull(message = "종료 시간은 필수입니다.") LocalTime endTime) {
}
