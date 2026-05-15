package com.artsync.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** 단건 슬롯 생성 요청. capacity 미지정 시 기본 4명. */
public record CreateSlotRequest(
        @NotNull(message = "날짜는 필수입니다.") LocalDate date,
        @NotNull(message = "시작 시간은 필수입니다.") LocalTime startTime,
        @NotNull(message = "종료 시간은 필수입니다.") LocalTime endTime,
        Integer capacity) {
}
