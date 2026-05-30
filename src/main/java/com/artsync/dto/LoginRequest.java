package com.artsync.dto;

import jakarta.validation.constraints.NotBlank;

/** 로그인 요청 — 아이디/비밀번호만. 역할은 가입 시 저장된 값을 자동으로 사용한다. */
public record LoginRequest(
        @NotBlank(message = "아이디는 필수입니다.") String loginId,
        @NotBlank(message = "비밀번호는 필수입니다.") String password) {
}
