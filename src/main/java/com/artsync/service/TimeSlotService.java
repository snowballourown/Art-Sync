package com.artsync.service;

import com.artsync.common.exception.BusinessException;
import com.artsync.common.exception.NotFoundException;
import com.artsync.domain.reservation.ReservationRepository;
import com.artsync.domain.reservation.ReservationStatus;
import com.artsync.domain.slot.TimeSlot;
import com.artsync.domain.slot.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 시간 슬롯 등록/관리 비즈니스 로직 (사장님 기능).
 * 대응 요구사항: FR-01, FR-02, FR-03, FR-11
 */
@Service
@Transactional(readOnly = true)
public class TimeSlotService {

    /** 슬롯 생성 시 capacity 미지정이면 적용하는 기본 수용 인원 */
    public static final int DEFAULT_CAPACITY = 4;

    private final TimeSlotRepository timeSlotRepository;
    private final ReservationRepository reservationRepository;
    private final UserService userService;

    public TimeSlotService(TimeSlotRepository timeSlotRepository,
                           ReservationRepository reservationRepository,
                           UserService userService) {
        this.timeSlotRepository = timeSlotRepository;
        this.reservationRepository = reservationRepository;
        this.userService = userService;
    }

    /**
     * 슬롯 일괄 생성 (FR-01, FR-02).
     * startTime ~ endTime 구간을 slotMinutes 단위로 잘라 여러 슬롯을 만든다.
     * 생성된 슬롯은 비공개(active=false) 상태이며, 별도로 activate 해야 회원에게 보인다.
     *
     * @param capacity null 또는 0 이하이면 DEFAULT_CAPACITY(4) 적용
     */
    @Transactional
    public List<Long> createSlots(Long adminId, LocalDate date,
                                  LocalTime startTime, LocalTime endTime,
                                  int slotMinutes, Integer capacity) {
        userService.requireAdmin(adminId);

        if (slotMinutes <= 0) {
            throw new BusinessException("슬롯 길이는 1분 이상이어야 합니다.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException("시작 시간은 종료 시간보다 빨라야 합니다.");
        }
        int appliedCapacity = (capacity == null || capacity <= 0) ? DEFAULT_CAPACITY : capacity;

        List<Long> createdIds = new ArrayList<>();
        LocalTime cursor = startTime;
        while (!cursor.plusMinutes(slotMinutes).isAfter(endTime)) {
            LocalTime slotEnd = cursor.plusMinutes(slotMinutes);
            TimeSlot slot = new TimeSlot(date, cursor, slotEnd, appliedCapacity, adminId);
            createdIds.add(timeSlotRepository.save(slot).getId());
            cursor = slotEnd;
        }
        if (createdIds.isEmpty()) {
            throw new BusinessException("주어진 시간 범위가 슬롯 길이보다 짧아 생성된 슬롯이 없습니다.");
        }
        return createdIds;
    }

    /** 단건 슬롯 생성 — 시간 범위를 직접 지정 */
    @Transactional
    public Long createSlot(Long adminId, LocalDate date,
                           LocalTime startTime, LocalTime endTime, Integer capacity) {
        userService.requireAdmin(adminId);
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException("시작 시간은 종료 시간보다 빨라야 합니다.");
        }
        int appliedCapacity = (capacity == null || capacity <= 0) ? DEFAULT_CAPACITY : capacity;
        TimeSlot slot = new TimeSlot(date, startTime, endTime, appliedCapacity, adminId);
        return timeSlotRepository.save(slot).getId();
    }

    /** 슬롯 시간 수정 (FR-02) */
    @Transactional
    public void updateSlotTime(Long adminId, Long slotId, LocalTime startTime, LocalTime endTime) {
        userService.requireAdmin(adminId);
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException("시작 시간은 종료 시간보다 빨라야 합니다.");
        }
        TimeSlot slot = getSlot(slotId);
        if (hasActiveReservation(slotId)) {
            throw new BusinessException("이미 예약이 있는 슬롯은 시간을 변경할 수 없습니다.");
        }
        slot.changeTime(startTime, endTime);
    }

    /** 슬롯 활성화 — 회원에게 공개 (FR-03) */
    @Transactional
    public void activate(Long adminId, Long slotId) {
        userService.requireAdmin(adminId);
        getSlot(slotId).activate();
    }

    /** 슬롯 비활성화 — 회원에게서 숨김 (FR-03) */
    @Transactional
    public void deactivate(Long adminId, Long slotId) {
        userService.requireAdmin(adminId);
        getSlot(slotId).deactivate();
    }

    /** 슬롯 삭제 — 진행 중인 예약이 있으면 막는다 */
    @Transactional
    public void deleteSlot(Long adminId, Long slotId) {
        userService.requireAdmin(adminId);
        TimeSlot slot = getSlot(slotId);
        if (hasActiveReservation(slotId)) {
            throw new BusinessException("이미 예약이 있는 슬롯은 삭제할 수 없습니다.");
        }
        timeSlotRepository.delete(slot);
    }


    /** 사장님용: 날짜 범위의 모든 슬롯 (월별 다이어리용) */
    public List<TimeSlot> getSlotsForAdminBetween(LocalDate from, LocalDate to) {
        return timeSlotRepository.findBySlotDateBetweenOrderBySlotDateAscStartTimeAsc(from, to);
    }

    /** 사장님용: 특정 날짜의 모든 슬롯 (활성/비활성 포함, FR-11) */
    public List<TimeSlot> getSlotsForAdmin(LocalDate date) {
        return timeSlotRepository.findBySlotDateOrderByStartTime(date);
    }

    /** 회원용: 특정 날짜의 활성화된 슬롯만 (FR-04) */
    public List<TimeSlot> getActiveSlots(LocalDate date) {
        return timeSlotRepository.findBySlotDateAndActiveTrueOrderByStartTime(date);
    }

    public TimeSlot getSlot(Long slotId) {
        return timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new NotFoundException("시간 슬롯을 찾을 수 없습니다. id=" + slotId));
    }

    /** 진행 중(REQUESTED/CONFIRMED)인 예약이 하나라도 있는지 */
    private boolean hasActiveReservation(Long slotId) {
        long requested = reservationRepository.countBySlotIdAndStatus(slotId, ReservationStatus.REQUESTED);
        long confirmed = reservationRepository.countBySlotIdAndStatus(slotId, ReservationStatus.CONFIRMED);
        return requested + confirmed > 0;
    }
}
