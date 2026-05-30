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
 * 시간 슬롯 등록/관리 비즈니스 로직.
 * 모든 쓰기 작업은 SpaceService.requireOwner() 로 공간 운영자 여부를 먼저 검증한다.
 */
@Service
@Transactional(readOnly = true)
public class TimeSlotService {

    public static final int DEFAULT_CAPACITY = 4;

    private final TimeSlotRepository timeSlotRepository;
    private final ReservationRepository reservationRepository;
    private final SpaceService spaceService;

    public TimeSlotService(TimeSlotRepository timeSlotRepository,
                           ReservationRepository reservationRepository,
                           SpaceService spaceService) {
        this.timeSlotRepository = timeSlotRepository;
        this.reservationRepository = reservationRepository;
        this.spaceService = spaceService;
    }

    /**
     * 슬롯 일괄 생성 (FR-01, FR-02).
     * startTime ~ endTime 구간을 slotMinutes 단위로 잘라 여러 슬롯을 만든다.
     * 생성된 슬롯은 비공개(active=false) 상태이며, activate() 해야 회원에게 보인다.
     */
    @Transactional
    public List<Long> createSlots(Long userId, Long spaceId, LocalDate date,
                                  LocalTime startTime, LocalTime endTime,
                                  int slotMinutes, Integer capacity) {
        spaceService.requireOwner(userId, spaceId);

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
            TimeSlot slot = new TimeSlot(spaceId, date, cursor, slotEnd, appliedCapacity, userId);
            createdIds.add(timeSlotRepository.save(slot).getId());
            cursor = slotEnd;
        }
        if (createdIds.isEmpty()) {
            throw new BusinessException("주어진 시간 범위가 슬롯 길이보다 짧아 생성된 슬롯이 없습니다.");
        }
        return createdIds;
    }

    /** 단건 슬롯 생성 */
    @Transactional
    public Long createSlot(Long userId, Long spaceId, LocalDate date,
                           LocalTime startTime, LocalTime endTime, Integer capacity) {
        spaceService.requireOwner(userId, spaceId);
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException("시작 시간은 종료 시간보다 빨라야 합니다.");
        }
        int appliedCapacity = (capacity == null || capacity <= 0) ? DEFAULT_CAPACITY : capacity;
        TimeSlot slot = new TimeSlot(spaceId, date, startTime, endTime, appliedCapacity, userId);
        return timeSlotRepository.save(slot).getId();
    }

    /** 슬롯 시간 수정 */
    @Transactional
    public void updateSlotTime(Long userId, Long spaceId, Long slotId,
                               LocalTime startTime, LocalTime endTime) {
        spaceService.requireOwner(userId, spaceId);
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException("시작 시간은 종료 시간보다 빨라야 합니다.");
        }
        TimeSlot slot = getSlot(slotId);
        if (hasActiveReservation(slotId)) {
            throw new BusinessException("이미 예약이 있는 슬롯은 시간을 변경할 수 없습니다.");
        }
        slot.changeTime(startTime, endTime);
    }

    /** 슬롯 활성화 — 참가자에게 공개 */
    @Transactional
    public void activate(Long userId, Long spaceId, Long slotId) {
        spaceService.requireOwner(userId, spaceId);
        getSlot(slotId).activate();
    }

    /** 슬롯 비활성화 — 참가자에게서 숨김 */
    @Transactional
    public void deactivate(Long userId, Long spaceId, Long slotId) {
        spaceService.requireOwner(userId, spaceId);
        getSlot(slotId).deactivate();
    }

    /** 슬롯 삭제 */
    @Transactional
    public void deleteSlot(Long userId, Long spaceId, Long slotId) {
        spaceService.requireOwner(userId, spaceId);
        TimeSlot slot = getSlot(slotId);
        if (hasActiveReservation(slotId)) {
            throw new BusinessException("이미 예약이 있는 슬롯은 삭제할 수 없습니다.");
        }
        timeSlotRepository.delete(slot);
    }

    /** 운영자용: 날짜 범위의 모든 슬롯 (월별 달력용) */
    public List<TimeSlot> getSlotsForOwnerBetween(Long spaceId, LocalDate from, LocalDate to) {
        return timeSlotRepository
                .findBySpaceIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(spaceId, from, to);
    }

    /** 운영자용: 특정 날짜의 모든 슬롯 (활성/비활성 포함) */
    public List<TimeSlot> getSlotsForOwner(Long spaceId, LocalDate date) {
        return timeSlotRepository.findBySpaceIdAndSlotDateOrderByStartTime(spaceId, date);
    }

    /** 참가자용: 특정 공간·날짜의 활성화된 슬롯만 */
    public List<TimeSlot> getActiveSlots(Long spaceId, LocalDate date) {
        return timeSlotRepository
                .findBySpaceIdAndSlotDateAndActiveTrueOrderByStartTime(spaceId, date);
    }

    public TimeSlot getSlot(Long slotId) {
        return timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new NotFoundException("시간 슬롯을 찾을 수 없습니다. id=" + slotId));
    }

    private boolean hasActiveReservation(Long slotId) {
        long requested = reservationRepository.countBySlotIdAndStatus(slotId, ReservationStatus.REQUESTED);
        long confirmed = reservationRepository.countBySlotIdAndStatus(slotId, ReservationStatus.CONFIRMED);
        return requested + confirmed > 0;
    }
}
