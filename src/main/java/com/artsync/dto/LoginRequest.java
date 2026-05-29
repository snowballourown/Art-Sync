package com.artsync.dto;

import com.artsync.common.web.SessionRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 로그인 요청 — 아이디/비밀번호와 함께 이번 세션의 역할을 선택한다. */
public record LoginRequest(
        @NotBlank(message = "아이디는 필수입니다.") String loginId,
        @NotBlank(message = "비밀번호는 필수입니다.") String password,
        @NotNull(message = "역할을 선택해주세요. (TEACHER 또는 PARTICIPANT)") SessionRole role) {
}
