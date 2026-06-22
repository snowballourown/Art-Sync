package com.artsync.service;

import com.artsync.common.exception.BusinessException;
import com.artsync.common.exception.NotFoundException;
import com.artsync.domain.notification.NotificationType;
import com.artsync.domain.reservation.Reservation;
import com.artsync.domain.reservation.ReservationRepository;
import com.artsync.domain.reservation.ReservationStatus;
import com.artsync.domain.slot.TimeSlot;
import com.artsync.domain.space.Space;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 예약 요청/수락/거절/취소 비즈니스 로직.
 * 수락·거절 권한은 "해당 슬롯이 속한 Space 의 운영자" 여부로 판단한다.
 */
@Service
@Transactional(readOnly = true)
public class ReservationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("M월 d일");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservationRepository reservationRepository;
    private final TimeSlotService timeSlotService;
    private final SpaceService spaceService;
    private final SpaceMemberService spaceMemberService;
    private final NotificationService notificationService;

    public ReservationService(ReservationRepository reservationRepository,
                              TimeSlotService timeSlotService,
                              SpaceService spaceService,
                              SpaceMemberService spaceMemberService,
                              NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.timeSlotService = timeSlotService;
        this.spaceService = spaceService;
        this.spaceMemberService = spaceMemberService;
        this.notificationService = notificationService;
    }

    /**
     * 참가자의 예약 요청.
     * 검증 순서: 공개 여부 → 마감(BR-01) → 정원(BR-02) → 중복 요청.
     * 성공 시 공간 운영자에게 알림을 생성한다.
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

        // 같은 날 다른 슬롯과 시간이 겹치는지 확인
        long overlapping = reservationRepository.countOverlappingForMember(
                memberId,
                List.of(ReservationStatus.REQUESTED, ReservationStatus.CONFIRMED),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime());
        if (overlapping > 0) {
            throw new BusinessException(
                    "같은 날 " + slot.getStartTime().toString().substring(0, 5)
                    + "~" + slot.getEndTime().toString().substring(0, 5)
                    + " 시간대와 겹치는 예약이 이미 있습니다.");
        }

        // 수업 코드를 입력해 등록한 참여자만 예약할 수 있다.
        spaceMemberService.requireMember(slot.getSpaceId(), memberId);
        YearMonth slotMonth = YearMonth.from(slot.getSlotDate());
        long monthlyUsed = spaceMemberService.getMonthlyUsed(memberId, slot.getSpaceId(), slotMonth);
        int monthlyLimit = spaceMemberService.getEffectiveMonthlyLimit(memberId, slot.getSpaceId(), slotMonth);
        if (monthlyUsed >= monthlyLimit) {
            throw new BusinessException(
                    slotMonth.getYear() + "년 " + slotMonth.getMonthValue()
                    + "월 수업 신청 한도(이월 포함 " + monthlyLimit + "회)를 모두 사용했습니다.");
        }

        Reservation reservation = new Reservation(slotId, memberId, memo);
        Long reservationId = reservationRepository.save(reservation).getId();

        // 공간 운영자에게 알림
        Space space = spaceService.getSpace(slot.getSpaceId());
        notificationService.create(
                space.getOwnerId(),
                NotificationType.RESERVATION_REQUESTED,
                "새 예약 요청이 도착했어요. " + describe(slot),
                reservationId);

        return reservationId;
    }

    /**
     * 운영자의 예약 수락.
     * 해당 슬롯이 속한 Space 의 운영자인지 검증한다.
     */
    @Transactional
    public void confirm(Long userId, Long reservationId) {
        Reservation reservation = getReservation(reservationId);
        TimeSlot slot = timeSlotService.getSlot(reservation.getSlotId());

        // 이 슬롯이 속한 공간의 운영자인지 확인
        spaceService.requireOwner(userId, slot.getSpaceId());

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

        if (confirmedCount + 1 >= slot.getCapacity()) {
            slot.markFull();
        }

        notificationService.create(
                reservation.getMemberId(),
                NotificationType.RESERVATION_CONFIRMED,
                "예약이 확정되었어요. " + describe(slot),
                reservationId);
    }

    /**
     * 운영자의 예약 거절.
     */
    @Transactional
    public void reject(Long userId, Long reservationId, String reason) {
        Reservation reservation = getReservation(reservationId);
        TimeSlot slot = timeSlotService.getSlot(reservation.getSlotId());

        spaceService.requireOwner(userId, slot.getSpaceId());

        reservation.reject(reason);

        String reasonText = (reason == null || reason.isBlank()) ? "" : " 사유: " + reason;
        notificationService.create(
                reservation.getMemberId(),
                NotificationType.RESERVATION_REJECTED,
                "예약 요청이 거절되었어요. " + describe(slot) + reasonText,
                reservationId);
    }

    /**
     * 예약 취소. 본인 예약만, 마감 전까지 취소할 수 있다.
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

        if (wasConfirmed) {
            slot.markAvailable();
        }

        Space space = spaceService.getSpace(slot.getSpaceId());
        notificationService.create(
                space.getOwnerId(),
                NotificationType.RESERVATION_CANCELLED,
                "참가자가 예약을 취소했어요. " + describe(slot),
                reservationId);
    }

    /** 참가자용: 내 예약 목록 */
    public List<Reservation> getMyReservations(Long memberId) {
        return reservationRepository.findByMemberIdOrderByRequestedAtDesc(memberId);
    }

    /** 운영자용: 특정 공간의 처리 대기(REQUESTED) 예약 목록 */
    public List<Reservation> getPendingReservations(Long userId, Long spaceId) {
        spaceService.requireOwner(userId, spaceId);
        return reservationRepository.findPendingBySpaceId(spaceId);
    }

    /** 운영자용: 여러 슬롯 ID + 상태 목록으로 예약 조회 */
    public List<Reservation> getReservationsBySlotIds(List<Long> slotIds,
                                                       List<ReservationStatus> statuses) {
        if (slotIds.isEmpty()) return List.of();
        return reservationRepository.findBySlotIdInAndStatusIn(slotIds, statuses);
    }

    public Reservation getReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다. id=" + reservationId));
    }

    public long confirmedCount(Long slotId) {
        return reservationRepository.countBySlotIdAndStatus(slotId, ReservationStatus.CONFIRMED);
    }

    private String describe(TimeSlot slot) {
        return slot.getSlotDate().format(DATE_FMT) + " "
                + slot.getStartTime().format(TIME_FMT) + "~"
                + slot.getEndTime().format(TIME_FMT);
    }
}
