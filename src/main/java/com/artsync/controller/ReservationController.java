package com.artsync.controller;

import com.artsync.common.web.SessionUtil;
import com.artsync.domain.reservation.Reservation;
import com.artsync.dto.IdResponse;
import com.artsync.dto.ReservationCreateRequest;
import com.artsync.dto.ReservationResponse;
import com.artsync.service.ReservationService;
import com.artsync.service.TimeSlotService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 회원용 예약 API (설계문서 3.3).
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final TimeSlotService timeSlotService;

    public ReservationController(ReservationService reservationService,
                                 TimeSlotService timeSlotService) {
        this.reservationService = reservationService;
        this.timeSlotService = timeSlotService;
    }

    /** 예약 요청 (FR-05) */
    @PostMapping
    public IdResponse request(@Valid @RequestBody ReservationCreateRequest request,
                              HttpSession session) {
        Long memberId = SessionUtil.currentUserId(session);
        Long id = reservationService.request(request.slotId(), memberId, request.memo());
        return new IdResponse(id);
    }

    /** 내 예약 목록 + 상태 조회 (FR-10) */
    @GetMapping("/me")
    public List<ReservationResponse> myReservations(HttpSession session) {
        Long memberId = SessionUtil.currentUserId(session);
        return reservationService.getMyReservations(memberId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** 예약 취소 (FR-12) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id, HttpSession session) {
        Long memberId = SessionUtil.currentUserId(session);
        reservationService.cancel(memberId, id);
        return ResponseEntity.ok().build();
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return ReservationResponse.of(
                reservation,
                timeSlotService.getSlot(reservation.getSlotId()));
    }
}
