package com.artsync.controller;

import com.artsync.dto.SlotResponse;
import com.artsync.service.ReservationService;
import com.artsync.service.TimeSlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 회원용 시간 슬롯 조회 API (설계문서 3.3).
 * 활성화된 슬롯만 노출한다 (BR-04).
 */
@RestController
@RequestMapping("/api/slots")
public class SlotController {

    private final TimeSlotService timeSlotService;
    private final ReservationService reservationService;

    public SlotController(TimeSlotService timeSlotService,
                          ReservationService reservationService) {
        this.timeSlotService = timeSlotService;
        this.reservationService = reservationService;
    }

    /**
     * 특정 날짜의 예약 가능(활성화된) 슬롯 조회 (FR-04).
     * 마감 기간(BR-01: 예약일 전날 23:59:59 초과)이 지난 슬롯은 반환하지 않는다.
     */
    @GetMapping
    public List<SlotResponse> getActiveSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        return timeSlotService.getActiveSlots(date).stream()
                .filter(slot -> !slot.isClosed(now))   // 마감된 슬롯 제외
                .map(slot -> SlotResponse.of(slot, reservationService.confirmedCount(slot.getId())))
                .toList();
    }
}
