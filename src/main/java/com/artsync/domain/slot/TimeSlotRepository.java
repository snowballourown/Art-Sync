package com.artsync.domain.slot;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

/**
 * TimeSlot 엔터티 영속성 처리.
 */
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    /** 사장님용: 특정 날짜의 모든 슬롯 (활성/비활성 포함) */
    List<TimeSlot> findBySlotDateOrderByStartTime(LocalDate slotDate);

    /** 회원용: 특정 날짜의 활성화된 슬롯만 */
    List<TimeSlot> findBySlotDateAndActiveTrueOrderByStartTime(LocalDate slotDate);
}
