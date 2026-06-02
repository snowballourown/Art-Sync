package com.artsync.controller;

import com.artsync.common.web.SessionUtil;
import com.artsync.dto.MyLimitResponse;
import com.artsync.dto.SpaceMemberResponse;
import com.artsync.dto.UpdateMemberLimitRequest;
import com.artsync.service.SpaceMemberService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 수업 참여자 관리 API.
 * GET  /api/spaces/{spaceId}/members           — 회원 현황판 (선생님 전용)
 * PATCH /api/spaces/{spaceId}/members/{memberId}/limit — 월간 한도 수정 (선생님 전용)
 */
@RestController
@RequestMapping("/api/spaces/{spaceId}/members")
public class SpaceMemberController {

    private final SpaceMemberService spaceMemberService;

    public SpaceMemberController(SpaceMemberService spaceMemberService) {
        this.spaceMemberService = spaceMemberService;
    }

    /** 회원 현황판: 참여자 목록 + 이번 달 사용/한도 */
    @GetMapping
    public List<SpaceMemberResponse> list(@PathVariable Long spaceId, HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        return spaceMemberService.getMemberStats(userId, spaceId);
    }

    /** 참여자용: 이번 달 내 사용 횟수 + 한도 조회 */
    @GetMapping("/my-limit")
    public MyLimitResponse myLimit(@PathVariable Long spaceId, HttpSession session) {
        SessionUtil.currentUserId(session); // 로그인 여부만 확인
        Long memberId = SessionUtil.currentUserId(session);
        long used  = spaceMemberService.getMonthlyUsed(memberId, spaceId);
        int  limit = spaceMemberService.getMonthlyLimit(memberId, spaceId);
        return new MyLimitResponse(used, limit);
    }

    /** 특정 참여자 월간 한도 수정 */
    @PatchMapping("/{memberId}/limit")
    public ResponseEntity<Void> updateLimit(@PathVariable Long spaceId,
                                            @PathVariable Long memberId,
                                            @Valid @RequestBody UpdateMemberLimitRequest request,
                                            HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        spaceMemberService.updateLimit(userId, spaceId, memberId, request.monthlyLimit());
        return ResponseEntity.ok().build();
    }
}
