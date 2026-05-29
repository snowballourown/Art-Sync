package com.artsync.domain.slot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * TimeSlot 엔터티 영속성 처리.
 * 모든 조회는 spaceId 로 범위를 한정한다.
 */
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    /** 운영자용: 특정 공간·날짜의 모든 슬롯 (활성/비활성 포함) */
    List<TimeSlot> findBySpaceIdAndSlotDateOrderByStartTime(Long spaceId, LocalDate slotDate);

    /** 참가자용: 특정 공간·날짜의 활성화된 슬롯만 */
    List<TimeSlot> findBySpaceIdAndSlotDateAndActiveTrueOrderByStartTime(Long spaceId, LocalDate slotDate);

    /** 운영자용: 날짜 범위의 모든 슬롯 (월별 달력용) */
    List<TimeSlot> findBySpaceIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(
            Long spaceId, LocalDate from, LocalDate to);

    /** 참가자용: 특정 날짜에 활성 슬롯이 있는 공간 목록 (브라우징용) */
    @Query("SELECT DISTINCT t.spaceId FROM TimeSlot t WHERE t.slotDate = :date AND t.active = true")
    List<Long> findActiveSpaceIdsByDate(@Param("date") LocalDate date);

    /** 특정 공간·날짜의 겹치는 슬롯 수 (중복 생성 방지) */
    @Query("SELECT COUNT(t) FROM TimeSlot t " +
           "WHERE t.spaceId = :spaceId " +
           "AND t.slotDate = :date " +
           "AND t.startTime < :endTime " +
           "AND t.endTime > :startTime")
    long countOverlapping(@Param("spaceId") Long spaceId,
                          @Param("date") LocalDate date,
                          @Param("startTime") LocalTime startTime,
                          @Param("endTime") LocalTime endTime);
}
