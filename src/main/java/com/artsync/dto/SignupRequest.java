package com.artsync.dto;

import com.artsync.domain.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 회원 가입 요청 */
public record SignupRequest(
        @NotBlank(message = "아이디는 필수입니다.") String loginId,
        @NotBlank(message = "비밀번호는 필수입니다.") String password,
        @NotBlank(message = "이름은 필수입니다.") String name,
        String phone,
        @NotNull(message = "역할을 선택해주세요. (TEACHER 또는 PARTICIPANT)") Role role) {
}
