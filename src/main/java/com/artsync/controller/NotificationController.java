package com.artsync.controller;

import com.artsync.common.web.SessionUtil;
import com.artsync.dto.NotificationResponse;
import com.artsync.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 알림 API (설계문서 3.5). 사장님·회원 공통.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 내 알림 목록 (안 읽은 것 우선) */
    @GetMapping("/me")
    public List<NotificationResponse> myNotifications(HttpSession session) {
        Long userId = SessionUtil.currentUserId(session);
        return notificationService.getMyNotifications(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /** 안 읽은 알림 개수 (뱃지용) */
    @GetMapping("/me/unread-count")
    public Map<String, Long> unreadCount(HttpSession session) {
        Long userId = SessionUtil.currentUserId(session);
        return Map.of("unreadCount", notificationService.getUnreadCount(userId));
    }

    /** 알림 읽음 처리 */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, HttpSession session) {
        SessionUtil.currentUserId(session); // 로그인 확인
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}
