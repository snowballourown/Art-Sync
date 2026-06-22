package com.artsync.dto;

import jakarta.validation.constraints.NotBlank;

/** 수업 코드 등록 요청 */
public record JoinSpaceRequest(
        @NotBlank(message = "수업 코드는 필수입니다.") String joinCode) {
}
