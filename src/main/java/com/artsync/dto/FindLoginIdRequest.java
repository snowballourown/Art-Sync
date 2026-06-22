package com.artsync.dto;

import jakarta.validation.constraints.NotBlank;

/** 아이디 찾기 요청 */
public record FindLoginIdRequest(
        @NotBlank(message = "이름은 필수입니다.") String name,
        @NotBlank(message = "연락처는 필수입니다.") String phone,
        @NotBlank(message = "복구 질문은 필수입니다.") String securityQuestion,
        @NotBlank(message = "복구 질문 답변은 필수입니다.") String securityAnswer) {
}
