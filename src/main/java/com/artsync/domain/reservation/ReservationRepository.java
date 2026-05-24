package com.artsync.domain.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Reservation 엔터티 영속성 처리.
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** 회원용: 내 예약 목록 (최신순) */
    List<Reservation> findByMemberIdOrderByRequestedAtDesc(Long memberId);

    /** 사장님용: 상태별 예약 목록 (예: REQUESTED 처리 대기 목록) */
    List<Reservation> findByStatusOrderByRequestedAtAsc(ReservationStatus status);

    /** 특정 슬롯의 상태별 예약 목록 (정원/중복 검증용) */
    List<Reservation> findBySlotIdAndStatus(Long slotId, ReservationStatus status);

    /** 동일 회원이 같은 슬롯에 이미 진행 중인 예약이 있는지 확인 (중복 요청 방지) */
    boolean existsBySlotIdAndMemberIdAndStatusIn(Long slotId, Long memberId,
                                                 List<ReservationStatus> statuses);

    /** 특정 슬롯의 확정 건수 (정원 검증용) */
    long countBySlotIdAndStatus(Long slotId, ReservationStatus status);

    /** 여러 슬롯 ID + 복수 상태 조건으로 예약 목록 조회 (관리자 대시보드용) */
    List<Reservation> findBySlotIdInAndStatusIn(List<Long> slotIds, List<ReservationStatus> statuses);
}
