package com.artsync.common.web;

import com.artsync.common.exception.BusinessException;
import jakarta.servlet.http.HttpSession;

/**
 * 세션 기반 로그인 상태·역할 관리 유틸리티.
 *
 * 세션에 저장되는 값
 *   LOGIN_USER_ID  - 로그인한 사용자의 DB id (Long)
 *   SESSION_ROLE   - 이 세션에서 선택한 역할 (SessionRole: TEACHER | PARTICIPANT)
 */
public final class SessionUtil {

    public static final String LOGIN_USER_ID = "LOGIN_USER_ID";
    public static final String SESSION_ROLE  = "SESSION_ROLE";

    private SessionUtil() {}

    /** 로그인 — userId 와 선택한 역할을 함께 저장 */
    public static void login(HttpSession session, Long userId, SessionRole role) {
        session.setAttribute(LOGIN_USER_ID, userId);
        session.setAttribute(SESSION_ROLE,  role);
    }

    public static void logout(HttpSession session) {
        session.invalidate();
    }

    /** 현재 로그인한 사용자 id. 비로그인 시 예외. */
    public static Long currentUserId(HttpSession session) {
        Object value = session.getAttribute(LOGIN_USER_ID);
        if (value == null) {
            throw new BusinessException("로그인이 필요합니다.");
        }
        return (Long) value;
    }

    /** 현재 세션 역할 반환. 로그인 안 됐거나 역할이 없으면 예외. */
    public static SessionRole currentRole(HttpSession session) {
        currentUserId(session); // 로그인 여부 먼저 확인
        Object role = session.getAttribute(SESSION_ROLE);
        if (role == null) {
            throw new BusinessException("세션 역할이 설정되지 않았습니다.");
        }
        return (SessionRole) role;
    }

    /**
     * 현재 세션이 선생님 역할인지 검증한다.
     * 선생님이 아니면 BusinessException.
     */
    public static void requireTeacher(HttpSession session) {
        if (currentRole(session) != SessionRole.TEACHER) {
            throw new BusinessException("선생님으로 로그인한 경우에만 사용할 수 있는 기능입니다.");
        }
    }

    /** 현재 세션이 선생님인지 여부 (boolean) */
    public static boolean isTeacher(HttpSession session) {
        try {
            return currentRole(session) == SessionRole.TEACHER;
        } catch (Exception e) {
            return false;
        }
    }
}
