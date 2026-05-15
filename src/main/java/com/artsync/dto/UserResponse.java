package com.artsync.dto;

import com.artsync.domain.user.Role;
import com.artsync.domain.user.User;

/** 사용자 정보 응답 (비밀번호 제외) */
public record UserResponse(Long id, String loginId, String name, String phone, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getLoginId(),
                user.getName(),
                user.getPhone(),
                user.getRole());
    }
}
