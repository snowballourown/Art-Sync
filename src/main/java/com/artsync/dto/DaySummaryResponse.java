package com.artsync.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 월별 다이어리용 하루 요약.
 * 해당 날짜의 슬롯 수와 예약 목록(예약자 이름 포함)을 담는다.
 */
public record DaySummaryResponse(
        LocalDate date,
        int slotCount,
        List<ReservationResponse> reservations
) {}
