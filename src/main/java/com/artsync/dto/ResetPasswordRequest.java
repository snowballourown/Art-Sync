package com.artsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 비밀번호 재설정 요청 */
public record ResetPasswordRequest(
        @NotBlank(message = "아이디는 필수입니다.") String loginId,
        @NotBlank(message = "이름은 필수입니다.") String name,
        @NotBlank(message = "연락처는 필수입니다.") String phone,
        @NotBlank(message = "복구 질문은 필수입니다.") String securityQuestion,
        @NotBlank(message = "복구 질문 답변은 필수입니다.") String securityAnswer,
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 6, message = "새 비밀번호는 6자 이상이어야 합니다.") String newPassword) {
}
