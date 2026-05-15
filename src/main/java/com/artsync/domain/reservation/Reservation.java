package com.artsync.domain.reservation;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 회원의 예약 요청 및 그 처리 결과.
 * 설계문서 2.2 - reservations 테이블 대응.
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 예약 대상 시간 슬롯 id */
    @Column(nullable = false)
    private Long slotId;

    /** 예약을 요청한 회원 id */
    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ReservationStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    /** 사장님이 수락/거절한 시각 (미처리 시 null) */
    @Column
    private LocalDateTime decidedAt;

    /** 회원 요청 메모 또는 사장님 거절 사유 */
    @Column(length = 255)
    private String memo;

    protected Reservation() {
        // JPA 기본 생성자
    }

    public Reservation(Long slotId, Long memberId, String memo) {
        this.slotId = slotId;
        this.memberId = memberId;
        this.memo = memo;
        this.status = ReservationStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    /** 사장님이 최종 수락 (FR-07) */
    public void confirm() {
        validatePending();
        this.status = ReservationStatus.CONFIRMED;
        this.decidedAt = LocalDateTime.now();
    }

    /** 사장님이 거절 (FR-07) */
    public void reject(String reason) {
        validatePending();
        this.status = ReservationStatus.REJECTED;
        this.decidedAt = LocalDateTime.now();
        this.memo = reason;
    }

    /** 마감 전 취소 (FR-12, 선택 기능) */
    public void cancel() {
        if (this.status != ReservationStatus.REQUESTED
                && this.status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("취소할 수 없는 예약 상태입니다: " + this.status);
        }
        this.status = ReservationStatus.CANCELLED;
        this.decidedAt = LocalDateTime.now();
    }

    private void validatePending() {
        if (this.status != ReservationStatus.REQUESTED) {
            throw new IllegalStateException("이미 처리된 예약입니다: " + this.status);
        }
    }

    public boolean isActive() {
        return status == ReservationStatus.REQUESTED || status == ReservationStatus.CONFIRMED;
    }

    public Long getId() {
        return id;
    }

    public Long getSlotId() {
        return slotId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public String getMemo() {
        return memo;
    }
}
