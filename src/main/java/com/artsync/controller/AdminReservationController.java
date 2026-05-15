package com.artsync.controller;

import com.artsync.common.web.SessionUtil;
import com.artsync.domain.reservation.Reservation;
import com.artsync.dto.RejectRequest;
import com.artsync.dto.ReservationResponse;
import com.artsync.service.ReservationService;
import com.artsync.service.TimeSlotService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사장님용 예약 처리 API (설계문서 3.4).
 * 권한 검증(ADMIN)은 서비스 계층에서 수행한다.
 */
@RestController
@RequestMapping("/api/admin/reservations")
public class AdminReservationController {

    private final ReservationService reservationService;
    private final TimeSlotService timeSlotService;

    public AdminReservationController(ReservationService reservationService,
                                      TimeSlotService timeSlotService) {
        this.reservationService = reservationService;
        this.timeSlotService = timeSlotService;
    }

    /** 처리 대기(REQUESTED) 예약 목록 (UC-03) */
    @GetMapping
    public List<ReservationResponse> pending(HttpSession session) {
        Long adminId = SessionUtil.currentUserId(session);
        return reservationService.getPendingReservations(adminId).stream()
                .map(this::toResponse)
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

    private ReservationResponse toResponse(Reservation reservation) {
        return ReservationResponse.of(
                reservation,
                timeSlotService.getSlot(reservation.getSlotId()));
    }
}
