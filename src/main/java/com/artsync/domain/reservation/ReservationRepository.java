package com.artsync.domain.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Reservation 엔터티 영속성 처리.
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** 참가자용: 내 예약 목록 (최신순) */
    List<Reservation> findByMemberIdOrderByRequestedAtDesc(Long memberId);

    /** 특정 슬롯의 상태별 예약 목록 (정원/중복 검증용) */
    List<Reservation> findBySlotIdAndStatus(Long slotId, ReservationStatus status);

    /** 동일 참가자가 같은 슬롯에 이미 진행 중인 예약이 있는지 확인 (중복 요청 방지) */
    boolean existsBySlotIdAndMemberIdAndStatusIn(Long slotId, Long memberId,
                                                 List<ReservationStatus> statuses);

    /** 특정 슬롯의 확정 건수 (정원 검증용) */
    long countBySlotIdAndStatus(Long slotId, ReservationStatus status);

    /** 여러 슬롯 ID + 복수 상태 조건으로 예약 목록 조회 (운영자 대시보드용) */
    List<Reservation> findBySlotIdInAndStatusIn(List<Long> slotIds, List<ReservationStatus> statuses);

    /**
     * 특정 참가자의 활성 예약 중 날짜·시간이 겹치는 건수.
     * Reservation ↔ TimeSlot 은 FK 매핑 없으므로 카르테시안 조인 후 id 조건으로 필터링.
     * 겹침 조건: existingStart < newEnd AND existingEnd > newStart
     */
    @Query("SELECT COUNT(r) FROM Reservation r, com.artsync.domain.slot.TimeSlot t " +
           "WHERE t.id = r.slotId " +
           "AND r.memberId = :memberId " +
           "AND r.status IN :statuses " +
           "AND t.slotDate = :date " +
           "AND t.startTime < :endTime " +
           "AND t.endTime > :startTime")
    long countOverlappingForMember(@Param("memberId") Long memberId,
                                   @Param("statuses") List<ReservationStatus> statuses,
                                   @Param("date") LocalDate date,
                                   @Param("startTime") LocalTime startTime,
                                   @Param("endTime") LocalTime endTime);

    /**
     * 특정 공간의 처리 대기(REQUESTED) 예약 목록.
     * Reservation ↔ TimeSlot 은 FK 관계가 아니므로 서브쿼리로 spaceId 를 필터링한다.
     */
    @Query("SELECT r FROM Reservation r " +
           "WHERE r.slotId IN " +
           "  (SELECT t.id FROM TimeSlot t WHERE t.spaceId = :spaceId) " +
           "AND r.status = com.artsync.domain.reservation.ReservationStatus.REQUESTED " +
           "ORDER BY r.requestedAt ASC")
    List<Reservation> findPendingBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 특정 참여자의 특정 수업 이번 달 예약 건수 (월간 한도 체크용).
     */
    @Query("SELECT COUNT(r) FROM Reservation r, com.artsync.domain.slot.TimeSlot t " +
           "WHERE t.id = r.slotId " +
           "AND r.memberId = :memberId " +
           "AND t.spaceId = :spaceId " +
           "AND r.status IN :statuses " +
           "AND YEAR(t.slotDate) = :year " +
           "AND MONTH(t.slotDate) = :month")
    long countMonthlyByMemberAndSpace(@Param("memberId") Long memberId,
                                      @Param("spaceId") Long spaceId,
                                      @Param("statuses") List<ReservationStatus> statuses,
                                      @Param("year") int year,
                                      @Param("month") int month);

    /**
     * 여러 참여자의 특정 수업 이번 달 예약 건수 (회원 현황판용).
     */
    @Query("SELECT r.memberId, COUNT(r) FROM Reservation r, com.artsync.domain.slot.TimeSlot t " +
           "WHERE t.id = r.slotId " +
           "AND t.spaceId = :spaceId " +
           "AND r.status IN :statuses " +
           "AND YEAR(t.slotDate) = :year " +
           "AND MONTH(t.slotDate) = :month " +
           "GROUP BY r.memberId")
    List<Object[]> countMonthlyBySpace(@Param("spaceId") Long spaceId,
                                       @Param("statuses") List<ReservationStatus> statuses,
                                       @Param("year") int year,
                                       @Param("month") int month);

    /**
     * 특정 공간의 진행 중 예약 건수 (수업 삭제 전 안전 검증용).
     */
    @Query("SELECT COUNT(r) FROM Reservation r " +
           "WHERE r.slotId IN " +
           "  (SELECT t.id FROM TimeSlot t WHERE t.spaceId = :spaceId) " +
           "AND r.status IN :statuses")
    long countActiveBySpaceId(@Param("spaceId") Long spaceId,
                              @Param("statuses") List<ReservationStatus> statuses);
}
