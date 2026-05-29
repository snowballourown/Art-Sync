package com.artsync.domain.slot;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 사장님이 생성한 예약 가능 시간대.
 * 설계문서 2.2 - time_slots 테이블 대응.
 */
@Entity
@Table(name = "time_slots")
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 슬롯이 속한 공간 */
    @Column(nullable = false)
    private Long spaceId;

    @Column(nullable = false)
    private LocalDate slotDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    /** 수용 인원. 기본 1명 (설계문서 0장 가정값) */
    @Column(nullable = false)
    private int capacity;

    /** 회원에게 공개되었는지 여부 (FR-03) */
    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private SlotStatus status;

    /** 슬롯을 생성한 사장님의 user id */
    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected TimeSlot() {
        // JPA 기본 생성자
    }

    public TimeSlot(Long spaceId, LocalDate slotDate, LocalTime startTime, LocalTime endTime,
                    int capacity, Long createdBy) {
        this.spaceId = spaceId;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.createdBy = createdBy;
        this.active = false;            // 생성 시에는 비공개 상태
        this.status = SlotStatus.AVAILABLE;
        this.createdAt = LocalDateTime.now();
    }

    /** 회원에게 공개 (FR-03) */
    public void activate() {
        this.active = true;
    }

    /** 회원에게서 숨김 (FR-03) */
    public void deactivate() {
        this.active = false;
    }

    public void changeTime(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void markFull() {
        this.status = SlotStatus.FULL;
    }

    public void markAvailable() {
        this.status = SlotStatus.AVAILABLE;
    }

    public void markClosed() {
        this.status = SlotStatus.CLOSED;
    }

    /**
     * 예약 마감 여부 판정 (FR-09 / BR-01).
     * 예약일 전날 23:59:59 가 지나면 마감.
     */
    public boolean isClosed(LocalDateTime now) {
        LocalDateTime deadline = slotDate.minusDays(1).atTime(23, 59, 59);
        return now.isAfter(deadline);
    }

    public Long getId() {
        return id;
    }

    public Long getSpaceId() {
        return spaceId;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isActive() {
        return active;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
