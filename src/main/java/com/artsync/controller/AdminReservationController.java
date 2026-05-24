package com.artsync.controller;

import com.artsync.common.web.SessionUtil;
import com.artsync.domain.reservation.Reservation;
import com.artsync.domain.reservation.ReservationStatus;
import com.artsync.domain.slot.TimeSlot;
import com.artsync.dto.RejectRequest;
import com.artsync.dto.ReservationResponse;
import com.artsync.service.ReservationService;
import com.artsync.service.TimeSlotService;
import com.artsync.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.artsync.dto.DaySummaryResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 사장님용 예약 처리 API (설계문서 3.4).
 * 권한 검증(ADMIN)은 서비스 계층에서 수행한다.
 */
@RestController
@RequestMapping("/api/admin/reservations")
public class AdminReservationController {

    private final ReservationService reservationService;
    private final TimeSlotService timeSlotService;
    private final UserService userService;

    public AdminReservationController(ReservationService reservationService,
                                      TimeSlotService timeSlotService,
                                      UserService userService) {
        this.reservationService = reservationService;
        this.timeSlotService = timeSlotService;
        this.userService = userService;
    }

    /** 처리 대기(REQUESTED) 예약 목록 (UC-03) */
    @GetMapping
    public List<ReservationResponse> pending(HttpSession session) {
        Long adminId = SessionUtil.currentUserId(session);
        return reservationService.getPendingReservations(adminId).stream()
                .map(this::toResponseWithName)
                .toList();
    }

    /**
     * 특정 날짜의 모든 활성 예약(REQUESTED + CONFIRMED) 목록.
     * 관리자 대시보드 "예약 현황"에서 날짜를 선택할 때 사용한다.
     */
    @GetMapping("/by-date")
    public List<ReservationResponse> byDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session) {
        Long adminId = SessionUtil.currentUserId(session);
        userService.requireAdmin(adminId);

        // 해당 날짜의 모든 슬롯 ID 수집
        List<Long> slotIds = timeSlotService.getSlotsForAdmin(date)
                .stream().map(TimeSlot::getId).toList();
        if (slotIds.isEmpty()) return List.of();

        // REQUESTED + CONFIRMED 예약만 조회
        return reservationService
                .getReservationsBySlotIds(slotIds,
                        List.of(ReservationStatus.REQUESTED, ReservationStatus.CONFIRMED))
                .stream()
                .map(this::toResponseWithName)
                .toList();
    }

    /** 예약 최종 수락 (FR-07) */
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable Long id, HttpSession session) {
        Long adminId = SessionUtil.currentUserId(session);
        reservationService.confirm(adminId, id);
        return ResponseEntity.ok().build();
    }

    /** 예약 거절 (FR-07) */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id,
                                       @RequestBody(required = false) RejectRequest request,
                                       HttpSession session) {
        Long adminId = SessionUtil.currentUserId(session);
        String reason = (request == null) ? null : request.reason();
        reservationService.reject(adminId, id, reason);
        return ResponseEntity.ok().build();
    }


    /**
     * 월간 예약 요약 — 달력 뷰용 (FR 기능 2).
     * 해당 월의 모든 날짜 중 슬롯이 있는 날짜만 반환한다.
     */
    @GetMapping("/monthly-summary")
    public List<DaySummaryResponse> monthlySummary(
            @RequestParam int year,
            @RequestParam int month,
            HttpSession session) {
        Long adminId = SessionUtil.currentUserId(session);
        userService.requireAdmin(adminId);

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        List<com.artsync.domain.slot.TimeSlot> allSlots =
                timeSlotService.getSlotsForAdminBetween(from, to);

        Map<LocalDate, List<com.artsync.domain.slot.TimeSlot>> slotsByDate = allSlots.stream()
                .collect(Collectors.groupingBy(com.artsync.domain.slot.TimeSlot::getSlotDate));

        List<Long> slotIds = allSlots.stream()
                .map(com.artsync.domain.slot.TimeSlot::getId).toList();

        List<com.artsync.domain.reservation.Reservation> reservations = slotIds.isEmpty()
                ? List.of()
                : reservationService.getReservationsBySlotIds(slotIds,
                        List.of(ReservationStatus.REQUESTED, ReservationStatus.CONFIRMED));

        Map<Long, List<com.artsync.domain.reservation.Reservation>> resBySlot = reservations.stream()
                .collect(Collectors.groupingBy(
                        com.artsync.domain.reservation.Reservation::getSlotId));

        List<DaySummaryResponse> result = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<com.artsync.domain.slot.TimeSlot> daySlots =
                    slotsByDate.getOrDefault(date, List.of());
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
            return "회원#" + memberId;
        }
    }
}
