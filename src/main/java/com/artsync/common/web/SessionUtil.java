package com.artsync.common.web;

import com.artsync.common.exception.BusinessException;
import jakarta.servlet.http.HttpSession;

/**
 * 세션 기반 로그인 상태 관리 유틸리티.
 * 정식 Spring Security 인증으로 교체하기 전까지 사용하는 단순 구현이다.
 */
public final class SessionUtil {

    public static final String LOGIN_USER_ID = "LOGIN_USER_ID";

    private SessionUtil() {
    }

    public static void login(HttpSession session, Long userId) {
        session.setAttribute(LOGIN_USER_ID, userId);
    }

    public static void logout(HttpSession session) {
        session.invalidate();
    }

    /** 현재 로그인한 사용자 id. 로그인되어 있지 않으면 예외. */
    public static Long currentUserId(HttpSession session) {
        Object value = session.getAttribute(LOGIN_USER_ID);
        if (value == null) {
            throw new BusinessException("로그인이 필요합니다.");
        }
        return (Long) value;
    }
}
