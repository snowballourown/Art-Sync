package com.artsync.dto;

/**
 * 회원 현황판에서 참여자 1명의 정보를 담는 응답 DTO.
 */
public record SpaceMemberResponse(
        Long memberId,
        String memberName,
        int monthlyLimit,
        long usedThisMonth,
        int carryover,
        int effectiveMonthlyLimit
) {}
