package com.artsync.controller;

import com.artsync.common.web.SessionUtil;
import com.artsync.dto.*;
import com.artsync.service.ReservationService;
import com.artsync.service.SpaceService;
import com.artsync.service.TimeSlotService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 수업별 시간 슬롯 관리 API.
 * 경로: /api/spaces/{spaceId}/slots
 *
 * 쓰기 작업 → requireTeacher() + requireOwner() 이중 검증
 * 읽기 작업 → ownerView=true 이면 선생님 + 소유권 검증, false 이면 누구나 활성 슬롯만 조회
 */
@RestController
@RequestMapping("/api/spaces/{spaceId}/slots")
public class SpaceSlotController {

    private final TimeSlotService timeSlotService;
    private final ReservationService reservationService;
    private final SpaceService spaceService;

    public SpaceSlotController(TimeSlotService timeSlotService,
                               ReservationService reservationService,
                               SpaceService spaceService) {
        this.timeSlotService = timeSlotService;
        this.reservationService = reservationService;
        this.spaceService = spaceService;
    }

    /** 슬롯 일괄 생성 (선생님 전용) */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<Long> createSlots(@PathVariable Long spaceId,
                                  @Valid @RequestBody CreateSlotsRequest request,
                                  HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        return timeSlotService.createSlots(userId, spaceId, request.date(),
                request.startTime(), request.endTime(),
                request.slotMinutes(), request.capacity());
    }

    /** 단건 슬롯 생성 (선생님 전용) */
    @PostMapping("/single")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse createSlot(@PathVariable Long spaceId,
                                 @Valid @RequestBody CreateSlotRequest request,
                                 HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        Long id = timeSlotService.createSlot(userId, spaceId, request.date(),
                request.startTime(), request.endTime(), request.capacity());
        return new IdResponse(id);
    }

    /**
     * 슬롯 목록 조회.
     * - ownerView=true  : 선생님 전용 — 활성/비활성 전체 (소유권 확인 포함)
     * - ownerView=false : 누구나  — 활성(공개) 슬롯만
     */
    @GetMapping
    public List<SlotResponse> getSlots(
            @PathVariable Long spaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean ownerView,
            HttpSession session) {

        if (ownerView) {
            SessionUtil.requireTeacher(session);
            Long userId = SessionUtil.currentUserId(session);
            spaceService.requireOwner(userId, spaceId);
            return timeSlotService.getSlotsForOwner(spaceId, date).stream()
                    .map(slot -> SlotResponse.of(slot, reservationService.confirmedCount(slot.getId())))
                    .toList();
        } else {
            return timeSlotService.getActiveSlots(spaceId, date).stream()
                    .map(slot -> SlotResponse.of(slot, reservationService.confirmedCount(slot.getId())))
                    .toList();
        }
    }

    /** 슬롯 시간 수정 (선생님 전용) */
    @PatchMapping("/{slotId}")
    public ResponseEntity<Void> updateTime(@PathVariable Long spaceId,
                                           @PathVariable Long slotId,
                                           @Valid @RequestBody UpdateSlotTimeRequest request,
                                           HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        timeSlotService.updateSlotTime(userId, spaceId, slotId,
                request.startTime(), request.endTime());
        return ResponseEntity.ok().build();
    }

    /** 슬롯 삭제 (선생님 전용) */
    @DeleteMapping("/{slotId}")
    public ResponseEntity<Void> delete(@PathVariable Long spaceId,
                                       @PathVariable Long slotId,
                                       HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        timeSlotService.deleteSlot(userId, spaceId, slotId);
        return ResponseEntity.ok().build();
    }

    /** 슬롯 활성화 — 참가자에게 공개 (선생님 전용) */
    @PatchMapping("/{slotId}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long spaceId,
                                         @PathVariable Long slotId,
                                         HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        timeSlotService.activate(userId, spaceId, slotId);
        return ResponseEntity.ok().build();
    }

    /** 슬롯 비활성화 — 참가자에게서 숨김 (선생님 전용) */
    @PatchMapping("/{slotId}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long spaceId,
                                           @PathVariable Long slotId,
                                           HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        timeSlotService.deactivate(userId, spaceId, slotId);
        return ResponseEntity.ok().build();
    }
}
