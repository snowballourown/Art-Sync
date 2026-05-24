package com.artsync.service;

import com.artsync.common.exception.BusinessException;
import com.artsync.common.exception.NotFoundException;
import com.artsync.domain.notification.NotificationType;
import com.artsync.domain.reservation.Reservation;
import com.artsync.domain.reservation.ReservationRepository;
import com.artsync.domain.reservation.ReservationStatus;
import com.artsync.domain.slot.TimeSlot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 예약 요청/수락/거절/취소 비즈니스 로직 — 시스템의 핵심.
 * 대응 요구사항: FR-05 ~ FR-10, FR-12 / 비즈니스 규칙 BR-01 ~ BR-04
 */
@Service
@Transactional(readOnly = true)
public class ReservationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("M월 d일");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservationRepository reservationRepository;
    private final TimeSlotService timeSlotService;
    private final NotificationService notificationService;
    private final UserService userService;

    public ReservationService(ReservationRepository reservationRepository,
                              TimeSlotService timeSlotService,
                              NotificationService notificationService,
                              UserService userService) {
        this.reservationRepository = reservationRepository;
        this.timeSlotService = timeSlotService;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    /**
     * 회원의 예약 요청 (FR-05).
     * 검증 순서: 공개 여부 → 마감(BR-01) → 정원(BR-02) → 중복 요청.
     * 성공 시 사장님에게 알림을 생성한다 (FR-06).
     */
    @Transactional
    public Long request(Long slotId, Long memberId, String memo) {
        TimeSlot slot = timeSlotService.getSlot(slotId);
        LocalDateTime now = LocalDateTime.now();

        if (!slot.isActive()) {
            throw new BusinessException("아직 공개되지 않은 시간대입니다.");
        }
        if (slot.isClosed(now)) {
            throw new BusinessException("예약이 마감되었습니다. 예약일 전날까지만 신청할 수 있습니다.");
        }
        long confirmedCount =
                reservationRepository.countBySlotIdAndStatus(slotId, ReservationStatus.CONFIRMED);
        if (confirmedCount >= slot.getCapacity()) {
            throw new BusinessException("이 시간대는 정원(" + slot.getCapacity() + "명)이 모두 찼습니다.");
        }
        boolean alreadyRequested = reservationRepository.existsBySlotIdAndMemberIdAndStatusIn(
                slotId, memberId, List.of(ReservationStatus.REQUESTED, ReservationStatus.CONFIRMED));
        if (alreadyRequested) {
            throw new BusinessException("이미 이 시간대에 신청한 예약이 있습니다.");
        }

        Reservation reservation = new Reservation(slotId, memberId, memo);
        Long reservationId = reservationRepository.save(reservation).getId();

        // 사장님에게 알림 (FR-06)
        notificationService.create(
                slot.getCreatedBy(),
                NotificationType.RESERVATION_REQUESTED,
                "새 예약 요청이 도착했어요. " + describe(slot),
                reservationId);

        return reservationId;
    }

    /**
     * 사장님의 예약 최종 수락 (FR-07).
     * 마감(BR-03)·정원(BR-02)을 다시 검증한 뒤 확정하고, 회원에게 알림을 보낸다 (FR-08).
     */
    @Transactional
    public void confirm(Long adminId, Long reservationId) {
        userService.requireAdmin(adminId);
        Reservation reservation = getReservation(reservationId);
        TimeSlot slot = timeSlotService.getSlot(reservation.getSlotId());
        LocalDateTime now = LocalDateTime.now();

        if (slot.isClosed(now)) {
            throw new BusinessException("마감된 시간대의 예약은 처리할 수 없습니다.");
        }
        long confirmedCount =
                reservationRepository.countBySlotIdAndStatus(slot.getId(), ReservationStatus.CONFIRMED);
        if (confirmedCount >= slot.getCapacity()) {
            throw new BusinessException("정원(" + slot.getCapacity() + "명)이 모두 차서 수락할 수 없습니다.");
        }

        reservation.confirm();

        // 이번 수락으로 정원이 찼다면 슬롯 상태 갱신
        if (confirmedCount + 1 >= slot.getCapacity()) {
            slot.markFull();
        }

        // 회원에게 알림 (FR-08)
        notificationService.create(
                reservation.getMemberId(),
                NotificationType.RESERVATION_CONFIRMED,
                "예약이 확정되었어요. " + describe(slot),
                reservationId);
    }

    /**
     * 사장님의 예약 거절 (FR-07). 회원에게 거절 사유와 함께 알림을 보낸다 (FR-08).
     */
    @Transactional
    public void reject(Long adminId, Long reservationId, String reason) {
        userService.requireAdmin(adminId);
        Reservation reservation = getReservation(reservationId);
        TimeSlot slot = timeSlotService.getSlot(reservation.getSlotId());

        reservation.reject(reason);

        String reasonText = (reason == null || reason.isBlank()) ? "" : " 사유: " + reason;
        notificationService.create(
                reservation.getMemberId(),
                NotificationType.RESERVATION_REJECTED,
                "예약 요청이 거절되었어요. " + describe(slot) + reasonText,
                reservationId);
    }

    /**
     * 예약 취소 (FR-12, 선택 기능). 본인 예약만, 마감 전까지 취소할 수 있다.
     * 확정 상태였다면 슬롯의 FULL 상태를 해제한다.
     */
    @Transactional
    public void cancel(Long memberId, Long reservationId) {
        Reservation reservation = getReservation(reservationId);
        if (!reservation.getMemberId().equals(memberId)) {
            throw new BusinessException("본인이 신청한 예약만 취소할 수 있습니다.");
        }
        TimeSlot slot = timeSlotService.getSlot(reservation.getSlotId());
        if (slot.isClosed(LocalDateTime.now())) {
            throw new BusinessException("마감된 예약은 취소할 수 없습니다.");
        }

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;
        reservation.cancel();

        // 확정됐던 예약이 취소되면 자리가 다시 비므로 슬롯을 예약 가능 상태로
        if (wasConfirmed) {
            slot.markAvailable();
        }

        notificationService.create(
                slot.getCreatedBy(),
                NotificationType.RESERVATION_CANCELLED,
                "회원이 예약을 취소했어요. " + describe(slot),
                reservationId);
    }

    /** 회원용: 내 예약 목록 + 상태 (FR-10) */
    public List<Reservation> getMyReservations(Long memberId) {
        return reservationRepository.findByMemberIdOrderByRequestedAtDesc(memberId);
    }

    /** 사장님용: 처리 대기(REQUESTED) 예약 목록 (UC-03) */
    public List<Reservation> getPendingReservations(Long adminId) {
        userService.requireAdmin(adminId);
        return reservationRepository.findByStatusOrderByRequestedAtAsc(ReservationStatus.REQUESTED);
    }

    /** 사장님용: 여러 슬롯 ID + 상태 목록으로 예약 조회 (대시보드용) */
    public List<Reservation> getReservationsBySlotIds(List<Long> slotIds,
                                                       List<ReservationStatus> statuses) {
        return reservationRepository.findBySlotIdInAndStatusIn(slotIds, statuses);
    }

    public Reservation getReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다. id=" + reservationId));
    }

    /** 특정 슬롯의 확정(CONFIRMED) 예약 인원 — 슬롯 응답의 '남은 자리' 계산에 사용 */
    public long confirmedCount(Long slotId) {
        return reservationRepository.countBySlotIdAndStatus(slotId, ReservationStatus.CONFIRMED);
    }

    /** 알림 메시지에 들어갈 슬롯 설명 (예: "5월 20일 14:00~16:00") */
    private String describe(TimeSlot slot) {
        return slot.getSlotDate().format(DATE_FMT) + " "
                + slot.getStartTime().format(TIME_FMT) + "~"
                + slot.getEndTime().format(TIME_FMT);
    }
}
