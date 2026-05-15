package com.artsync.controller;

import com.artsync.common.web.SessionUtil;
import com.artsync.dto.CreateSlotRequest;
import com.artsync.dto.CreateSlotsRequest;
import com.artsync.dto.IdResponse;
import com.artsync.dto.SlotResponse;
import com.artsync.dto.UpdateSlotTimeRequest;
import com.artsync.service.ReservationService;
import com.artsync.service.TimeSlotService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 사장님용 시간 슬롯 관리 API (설계문서 3.2).
 * 권한 검증(ADMIN)은 서비스 계층에서 수행한다.
 */
@RestController
@RequestMapping("/api/admin/slots")
public class AdminSlotController {

    private final TimeSlotService timeSlotService;
    private final ReservationService reservationService;

    public AdminSlotController(TimeSlotService timeSlotService,
                               ReservationService reservationService) {
        this.timeSlotService = timeSlotService;
        this.reservationService = reservationService;
    }

    /** 슬롯 일괄 생성 (FR-01, FR-02) */
    @PostMapping
    public List<Long> createSlots(@Valid @RequestBody CreateSlotsRequest request,
                                  HttpSession session) {
        Long adminId = SessionUtil.currentUserId(session);
        return timeSlotService.createSlots(adminId, request.date(),
                request.startTime(), request.endTime(),
                request.slotMinutes(), request.capacity());
    }

    /** 단건 슬롯 생성 */
    @PostMapping("/single")
    public IdResponse createSlot(@Valid @RequestBody CreateSlotRequest request,
                                 HttpSession session) {
        Long adminId = SessionUtil.currentUserId(session);
        Long id = timeSlotService.createSlot(adminId, request.date(),
                request.startTime(), request.endTime(), request.capacity());
        return new IdResponse(id);
    }

    /** 특정 날짜의 모든 슬롯 조회 (활성/비활성 포함, FR-11) */
    @GetMapping
    public List<SlotResponse> getSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return timeSlotService.getSlotsForAdmin(date).stream()
                .map(slot -> SlotResponse.of(slot, reservationService.confirmedCount(slot.getId())))
                .toList();
    }

    /** 슬롯 시간 수정 (FR-02) */
    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateTime(@PathVariable Long id,
                                           @Valid @RequestBody UpdateSlotTimeRequest request,
                                           HttpSession session) {
        Long adminId = SessionUtil.currentUserId(session);
        timeSlotService.updateSlotTime(adminId, id, request.startTime(), request.endTime());
        return ResponseEntity.ok().build();
    }

    /** 슬롯 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpSession session) {
        Long adminId = SessionUtil.currentUserId(session);
        timeSlotService.deleteSlot(adminId, id);
        return ResponseEntity.ok().build();
    }

    /** 슬롯 활성화 — 회원에게 공개 (FR-03) */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id, HttpSession session) {
        Long adminId = SessionUtil.currentUserId(session);
        timeSlotService.activate(adminId, id);
        return ResponseEntity.ok().build();
    }

    /** 슬롯 비활성화 — 회원에게서 숨김 (FR-03) */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id, HttpSession session) {
        Long adminId = SessionUtil.currentUserId(session);
        timeSlotService.deactivate(adminId, id);
        return ResponseEntity.ok().build();
    }
}
