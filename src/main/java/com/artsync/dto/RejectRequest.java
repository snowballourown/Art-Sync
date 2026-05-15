package com.artsync.dto;

/** 예약 거절 요청 (사장님). reason 은 선택. */
public record RejectRequest(String reason) {
}
