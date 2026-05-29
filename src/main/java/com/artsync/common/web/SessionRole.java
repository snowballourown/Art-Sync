package com.artsync.common.web;

/**
 * 로그인 시 사용자가 선택하는 세션 역할.
 * - TEACHER   : 수업 생성·슬롯 관리·예약 수락/거절
 * - PARTICIPANT: 열려 있는 수업 탐색·예약 요청/취소
 *
 * 동일한 계정으로 역할을 바꿔 가며 로그인할 수 있다.
 */
public enum SessionRole {
    TEACHER,
    PARTICIPANT
}
