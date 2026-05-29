package com.artsync.service;

import com.artsync.common.exception.BusinessException;
import com.artsync.domain.reservation.ReservationRepository;
import com.artsync.domain.slot.TimeSlot;
import com.artsync.domain.slot.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

/**
 * TimeSlotService 단위 테스트.
 * 핵심 검증 대상: 시간대 중복 방지 (FR-01 / FR-02) + 입력값 유효성.
 */
@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SpaceService spaceService;

    @InjectMocks
    private TimeSlotService timeSlotService;

    /** 테스트에 공통으로 사용하는 고정값 */
    private static final Long USER_ID  = 1L;
    private static final Long SPACE_ID = 10L;
    private static final LocalDate DATE = LocalDate.of(2025, 8, 1);

    @BeforeEach
    void setupOwnerStub() {
        // requireOwner() 는 void 메서드 — 아무것도 하지 않도록 stub
        willDoNothing().given(spaceService).requireOwner(USER_ID, SPACE_ID);
    }

    // ─────────────────────────────────────────────────────────────
    //  단건 슬롯 생성 (createSlot)
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("createSlot — 단건 슬롯 생성")
    class CreateSlot {

        @Test
        @DisplayName("겹치는 슬롯이 없으면 정상 생성된다")
        void createSlot_noOverlap_success() {
            LocalTime start = LocalTime.of(10, 0);
            LocalTime end   = LocalTime.of(11, 0);

            TimeSlot saved = mockSavedSlot(42L);
            given(timeSlotRepository.countOverlapping(SPACE_ID, DATE, start, end)).willReturn(0L);
            given(timeSlotRepository.save(any(TimeSlot.class))).willReturn(saved);

            Long slotId = timeSlotService.createSlot(USER_ID, SPACE_ID, DATE, start, end, 4);

            assertThat(slotId).isEqualTo(42L);
            then(timeSlotRepository).should().save(any(TimeSlot.class));
        }

        @Test
        @DisplayName("완전히 동일한 시간대가 이미 존재하면 BusinessException")
        void createSlot_exactSameTime_throwsBusinessException() {
            LocalTime start = LocalTime.of(10, 0);
            LocalTime end   = LocalTime.of(11, 0);

            given(timeSlotRepository.countOverlapping(SPACE_ID, DATE, start, end)).willReturn(1L);

            assertThatThrownBy(() ->
                    timeSlotService.createSlot(USER_ID, SPACE_ID, DATE, start, end, 4))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("겹칩니다");
        }

        @Test
        @DisplayName("새 슬롯의 시작 시간이 기존 슬롯 내부에 걸치면 BusinessException")
        void createSlot_newStartInsideExisting_throwsBusinessException() {
            LocalTime start = LocalTime.of(11, 0);
            LocalTime end   = LocalTime.of(13, 0);

            given(timeSlotRepository.countOverlapping(SPACE_ID, DATE, start, end)).willReturn(1L);

            assertThatThrownBy(() ->
                    timeSlotService.createSlot(USER_ID, SPACE_ID, DATE, start, end, 4))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("새 슬롯의 종료 시간이 기존 슬롯 내부에 걸치면 BusinessException")
        void createSlot_newEndInsideExisting_throwsBusinessException() {
            LocalTime start = LocalTime.of(9, 0);
            LocalTime end   = LocalTime.of(11, 0);

            given(timeSlotRepository.countOverlapping(SPACE_ID, DATE, start, end)).willReturn(1L);

            assertThatThrownBy(() ->
                    timeSlotService.createSlot(USER_ID, SPACE_ID, DATE, start, end, 4))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("새 슬롯이 기존 슬롯을 완전히 포함하면 BusinessException")
        void createSlot_newContainsExisting_throwsBusinessException() {
            LocalTime start = LocalTime.of(9, 0);
            LocalTime end   = LocalTime.of(12, 0);

            given(timeSlotRepository.countOverlapping(SPACE_ID, DATE, start, end)).willReturn(1L);

            assertThatThrownBy(() ->
                    timeSlotService.createSlot(USER_ID, SPACE_ID, DATE, start, end, 4))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("시작 시간 ≥ 종료 시간이면 BusinessException (시간 역전)")
        void createSlot_startAfterEnd_throwsBusinessException() {
            LocalTime start = LocalTime.of(12, 0);
            LocalTime end   = LocalTime.of(10, 0);

            assertThatThrownBy(() ->
                    timeSlotService.createSlot(USER_ID, SPACE_ID, DATE, start, end, 4))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("시작 시간은 종료 시간보다 빨라야");

            then(timeSlotRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("capacity 가 null 이면 DEFAULT_CAPACITY(4) 로 저장된다")
        void createSlot_nullCapacity_usesDefault() {
            LocalTime start = LocalTime.of(10, 0);
            LocalTime end   = LocalTime.of(11, 0);

            TimeSlot saved = mockSavedSlot(10L);
            given(timeSlotRepository.countOverlapping(SPACE_ID, DATE, start, end)).willReturn(0L);
            given(timeSlotRepository.save(any(TimeSlot.class))).willReturn(saved);

            Long slotId = timeSlotService.createSlot(USER_ID, SPACE_ID, DATE, start, end, null);

            assertThat(slotId).isEqualTo(10L);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  일괄 슬롯 생성 (createSlots)
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("createSlots — 일괄 슬롯 생성")
    class CreateSlots {

        @Test
        @DisplayName("10:00~12:00 를 60분 단위로 나누면 슬롯 2개가 생성된다")
        void createSlots_twoSlots_success() {
            LocalTime slot1Start = LocalTime.of(10, 0);
            LocalTime slot1End   = LocalTime.of(11, 0);
            LocalTime slot2Start = LocalTime.of(11, 0);
            LocalTime slot2End   = LocalTime.of(12, 0);

            TimeSlot saved1 = mockSavedSlot(1L);
            TimeSlot saved2 = mockSavedSlot(2L);
            given(timeSlotRepository.countOverlapping(SPACE_ID, DATE, slot1Start, slot1End)).willReturn(0L);
            given(timeSlotRepository.countOverlapping(SPACE_ID, DATE, slot2Start, slot2End)).willReturn(0L);
            given(timeSlotRepository.save(any(TimeSlot.class)))
                    .willReturn(saved1, saved2);

            List<Long> ids = timeSlotService.createSlots(USER_ID, SPACE_ID, DATE, slot1Start, slot2End, 60, 4);

            assertThat(ids).hasSize(2).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("시간 범위가 슬롯 길이보다 짧으면 BusinessException")
        void createSlots_rangeShorterThanSlot_throwsBusinessException() {
            LocalTime rangeStart = LocalTime.of(10, 0);
            LocalTime rangeEnd   = LocalTime.of(10, 30);

            assertThatThrownBy(() ->
                    timeSlotService.createSlots(USER_ID, SPACE_ID, DATE, rangeStart, rangeEnd, 60, 4))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("생성된 슬롯이 없습니다");
        }

        @Test
        @DisplayName("slotMinutes 가 0 이하이면 BusinessException")
        void createSlots_zeroSlotMinutes_throwsBusinessException() {
            assertThatThrownBy(() ->
                    timeSlotService.createSlots(USER_ID, SPACE_ID, DATE,
                            LocalTime.of(10, 0), LocalTime.of(12, 0), 0, 4))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("1분 이상");
        }

        @Test
        @DisplayName("일괄 생성 도중 특정 구간이 기존 슬롯과 겹치면 BusinessException")
        void createSlots_overlapInMiddle_throwsBusinessException() {
            LocalTime rangeStart = LocalTime.of(10, 0);
            LocalTime rangeEnd   = LocalTime.of(12, 0);

            TimeSlot saved1 = mockSavedSlot(1L);
            given(timeSlotRepository.countOverlapping(SPACE_ID, DATE, LocalTime.of(10, 0), LocalTime.of(11, 0)))
                    .willReturn(0L);
            given(timeSlotRepository.save(any(TimeSlot.class)))
                    .willReturn(saved1);
            given(timeSlotRepository.countOverlapping(SPACE_ID, DATE, LocalTime.of(11, 0), LocalTime.of(12, 0)))
                    .willReturn(1L);

            assertThatThrownBy(() ->
                    timeSlotService.createSlots(USER_ID, SPACE_ID, DATE, rangeStart, rangeEnd, 60, 4))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("겹칩니다");
        }

        @Test
        @DisplayName("시작 시간 ≥ 종료 시간이면 BusinessException")
        void createSlots_startAfterEnd_throwsBusinessException() {
            assertThatThrownBy(() ->
                    timeSlotService.createSlots(USER_ID, SPACE_ID, DATE,
                            LocalTime.of(12, 0), LocalTime.of(10, 0), 60, 4))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("시작 시간은 종료 시간보다 빨라야");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  헬퍼
    // ─────────────────────────────────────────────────────────────

    private TimeSlot mockSavedSlot(Long id) {
        TimeSlot slot = mock(TimeSlot.class);
        given(slot.getId()).willReturn(id);
        return slot;
    }
}
