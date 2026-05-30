package com.artsync.domain.user;

/**
 * 사용자 역할 — 가입 시 선택되며 이후 변경되지 않는다.
 * TEACHER     : 수업 생성·슬롯 관리·예약 수락/거절
 * PARTICIPANT : 수업 탐색·예약 요청/취소
 */
public enum Role {
    TEACHER,
    PARTICIPANT
}
