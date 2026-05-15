package com.artsync.domain.user;

/**
 * 사용자 권한.
 * ADMIN  - 사장님 (시간대 등록, 예약 최종 수락/거절)
 * MEMBER - 회원 (시간대 조회, 예약 요청)
 */
public enum Role {
    ADMIN,
    MEMBER
}
