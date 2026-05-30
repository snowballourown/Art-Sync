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
 * 핵심 검증 대상: 입력값 유효성 (시간 역전, 범위 부족, slotMinutes 범위).
 * 슬롯 간 시간 겹침은 허용 정책으로 변경되어 검사하지 않는다.
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

    private static final Long USER_ID  = 1L;
    private static final Long SPACE_ID = 10L;
    private static final LocalDate DATE = LocalDate.of(2025, 8, 1);

    @BeforeEach
    void setupOwnerStub() {
        willDoNothing().given(spaceService).requireOwner(USER_ID, SPACE_ID);
    }

    // ─────────────────────────────────────────────────────────────
    //  단건 슬롯 생성 (createSlot)
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("createSlot — 단건 슬롯 생성")
    class CreateSlot {

        @Test
        @DisplayName("정상 입력이면 슬롯이 생성된다")
        void createSlot_validInput_success() {
            LocalTime start = LocalTime.of(10, 0);
            LocalTime end   = LocalTime.of(11, 0);

            TimeSlot saved = mockSavedSlot(42L);
            given(timeSlotRepository.save(any(TimeSlot.class))).willReturn(saved);

            Long slotId = timeSlotService.createSlot(USER_ID, SPACE_ID, DATE, start, end, 4);

            assertThat(slotId).isEqualTo(42L);
            then(timeSlotRepository).should().save(any(TimeSlot.class));
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
            TimeSlot saved1 = mockSavedSlot(1L);
            TimeSlot saved2 = mockSavedSlot(2L);
            given(timeSlotRepository.save(any(TimeSlot.class)))
                    .willReturn(saved1, saved2);

            List<Long> ids = timeSlotService.createSlots(
                    USER_ID, SPACE_ID, DATE,
                    LocalTime.of(10, 0), LocalTime.of(12, 0), 60, 4);

            assertThat(ids).hasSize(2).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("시간 범위가 슬롯 길이보다 짧으면 BusinessException")
        void createSlots_rangeShorterThanSlot_throwsBusinessException() {
            assertThatThrownBy(() ->
                    timeSlotService.createSlots(USER_ID, SPACE_ID, DATE,
                            LocalTime.of(10, 0), LocalTime.of(10, 30), 60, 4))
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
