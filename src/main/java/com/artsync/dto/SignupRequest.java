package com.artsync.dto;

import jakarta.validation.constraints.NotBlank;

/** 회원 가입 요청 */
public record SignupRequest(
        @NotBlank(message = "아이디는 필수입니다.") String loginId,
        @NotBlank(message = "비밀번호는 필수입니다.") String password,
        @NotBlank(message = "이름은 필수입니다.") String name,
        String phone) {
}
