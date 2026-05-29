package com.artsync.controller;

import com.artsync.common.web.SessionUtil;
import com.artsync.domain.reservation.Reservation;
import com.artsync.domain.reservation.ReservationStatus;
import com.artsync.domain.slot.TimeSlot;
import com.artsync.dto.DaySummaryResponse;
import com.artsync.dto.RejectRequest;
import com.artsync.dto.ReservationResponse;
import com.artsync.service.ReservationService;
import com.artsync.service.SpaceService;
import com.artsync.service.TimeSlotService;
import com.artsync.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 공간별 예약 관리 API (운영자용).
 * 경로: /api/spaces/{spaceId}/reservations
 */
@RestController
@RequestMapping("/api/spaces/{spaceId}/reservations")
public class SpaceReservationController {

    private final ReservationService reservationService;
    private final TimeSlotService timeSlotService;
    private final SpaceService spaceService;
    private final UserService userService;

    public SpaceReservationController(ReservationService reservationService,
                                      TimeSlotService timeSlotService,
                                      SpaceService spaceService,
                                      UserService userService) {
        this.reservationService = reservationService;
        this.timeSlotService = timeSlotService;
        this.spaceService = spaceService;
        this.userService = userService;
    }

    /** 처리 대기(REQUESTED) 예약 목록 (선생님 전용) */
    @GetMapping
    public List<ReservationResponse> pending(@PathVariable Long spaceId, HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        return reservationService.getPendingReservations(userId, spaceId).stream()
                .map(r -> toResponseWithName(r))
                .toList();
    }

    /** 특정 날짜의 활성 예약(REQUESTED + CONFIRMED) 목록 (선생님 전용) */
    @GetMapping("/by-date")
    public List<ReservationResponse> byDate(
            @PathVariable Long spaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        spaceService.requireOwner(userId, spaceId);

        List<Long> slotIds = timeSlotService.getSlotsForOwner(spaceId, date)
                .stream().map(TimeSlot::getId).toList();

        return reservationService
                .getReservationsBySlotIds(slotIds,
                        List.of(ReservationStatus.REQUESTED, ReservationStatus.CONFIRMED))
                .stream()
                .map(r -> toResponseWithName(r))
                .toList();
    }

    /** 예약 수락 (선생님 전용) */
    @PatchMapping("/{reservationId}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable Long spaceId,
                                        @PathVariable Long reservationId,
                                        HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        reservationService.confirm(userId, reservationId);
        return ResponseEntity.ok().build();
    }

    /** 예약 거절 (선생님 전용) */
    @PatchMapping("/{reservationId}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long spaceId,
                                       @PathVariable Long reservationId,
                                       @RequestBody(required = false) RejectRequest request,
                                       HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        String reason = (request == null) ? null : request.reason();
        reservationService.reject(userId, reservationId, reason);
        return ResponseEntity.ok().build();
    }

    /** 월간 예약 요약 — 달력 뷰용 (선생님 전용) */
    @GetMapping("/monthly-summary")
    public List<DaySummaryResponse> monthlySummary(
            @PathVariable Long spaceId,
            @RequestParam int year,
            @RequestParam int month,
            HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        spaceService.requireOwner(userId, spaceId);

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        List<TimeSlot> allSlots = timeSlotService.getSlotsForOwnerBetween(spaceId, from, to);

        Map<LocalDate, List<TimeSlot>> slotsByDate = allSlots.stream()
                .collect(Collectors.groupingBy(TimeSlot::getSlotDate));

        List<Long> slotIds = allSlots.stream().map(TimeSlot::getId).toList();

        List<Reservation> reservations = reservationService.getReservationsBySlotIds(
                slotIds, List.of(ReservationStatus.REQUESTED, ReservationStatus.CONFIRMED));

        Map<Long, List<Reservation>> resBySlot = reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getSlotId));

        List<DaySummaryResponse> result = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<TimeSlot> daySlots = slotsByDate.getOrDefault(date, List.of());
            if (daySlots.isEmpty()) continue;

            List<ReservationResponse> dayReservations = daySlots.stream()
                    .flatMap(slot -> resBySlot.getOrDefault(slot.getId(), List.of()).stream()
                            .map(r -> ReservationResponse.of(r, slot, tryGetMemberName(r.getMemberId()))))
                    .toList();

            result.add(new DaySummaryResponse(date, daySlots.size(), dayReservations));
        }
        return result;
    }

    private ReservationResponse toResponseWithName(Reservation reservation) {
        String memberName = tryGetMemberName(reservation.getMemberId());
        return ReservationResponse.of(
                reservation,
                timeSlotService.getSlot(reservation.getSlotId()),
                memberName);
    }

    private String tryGetMemberName(Long memberId) {
        try {
            return userService.getById(memberId).getName();
        } catch (Exception e) {
            return "참가자#" + memberId;
        }
    }
}
