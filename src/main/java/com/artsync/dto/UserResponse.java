package com.artsync.dto;

import com.artsync.common.web.SessionRole;
import com.artsync.domain.user.User;

/**
 * 로그인 응답 / 사용자 정보 응답.
 * sessionRole — 이 세션에서 선택한 역할 (TEACHER | PARTICIPANT).
 *               /api/auth/me 에서는 세션 역할을 별도로 전달해야 한다.
 */
public record UserResponse(Long id, String loginId, String name, String phone,
                           SessionRole sessionRole) {

    /** 로그인 응답 — 역할 포함 */
    public static UserResponse from(User user, SessionRole sessionRole) {
        return new UserResponse(
                user.getId(),
                user.getLoginId(),
                user.getName(),
                user.getPhone(),
                sessionRole);
    }
}
