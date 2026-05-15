package com.artsync.dto;

import jakarta.validation.constraints.NotNull;

/** 예약 요청 (회원). memo 는 선택. */
public record ReservationCreateRequest(
        @NotNull(message = "슬롯 id 는 필수입니다.") Long slotId,
        String memo) {
}
