package com.artsync.domain.spacemember;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 수업(Space)에 등록된 참여자.
 * 참여자가 해당 수업에서 처음 예약을 신청하면 자동 생성된다.
 * 선생님이 월간 수업 횟수 한도를 개별 설정할 수 있다.
 */
@Entity
@Table(name = "space_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"space_id", "member_id"}))
public class SpaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 이 참여자의 월간 수업 신청 한도 (선생님이 개별 설정) */
    @Column(nullable = false)
    private int monthlyLimit;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    protected SpaceMember() {}

    public SpaceMember(Long spaceId, Long memberId, int monthlyLimit) {
        this.spaceId = spaceId;
        this.memberId = memberId;
        this.monthlyLimit = monthlyLimit;
        this.joinedAt = LocalDateTime.now();
    }

    public void updateMonthlyLimit(int monthlyLimit) {
        if (monthlyLimit < 0) throw new IllegalArgumentException("한도는 0 이상이어야 합니다.");
        this.monthlyLimit = monthlyLimit;
    }

    public Long getId() { return id; }
    public Long getSpaceId() { return spaceId; }
    public Long getMemberId() { return memberId; }
    public int getMonthlyLimit() { return monthlyLimit; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
