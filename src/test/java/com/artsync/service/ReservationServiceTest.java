package com.artsync.service;

import com.artsync.common.exception.BusinessException;
import com.artsync.domain.reservation.ReservationRepository;
import com.artsync.domain.slot.TimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TimeSlotService timeSlotService;

    @Mock
    private SpaceService spaceService;

    @Mock
    private SpaceMemberService spaceMemberService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReservationService reservationService;

    private static final Long SLOT_ID = 10L;
    private static final Long SPACE_ID = 20L;
    private static final Long MEMBER_ID = 30L;
    private static final Long TEACHER_ID = 40L;

    @Test
    @DisplayName("월간 신청 한도는 현재 달이 아니라 예약하려는 수업 달 기준으로 검사한다")
    void request_monthlyLimitUsesSlotMonth() {
        YearMonth slotMonth = YearMonth.now().plusMonths(1);
        LocalDate slotDate = slotMonth.atDay(10);
        TimeSlot slot = new TimeSlot(
                SPACE_ID,
                slotDate,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                4,
                TEACHER_ID);
        slot.activate();

        given(timeSlotService.getSlot(SLOT_ID)).willReturn(slot);
        given(spaceMemberService.getMonthlyUsed(eq(MEMBER_ID), eq(SPACE_ID), any(YearMonth.class)))
                .willAnswer(invocation -> slotMonth.equals(invocation.getArgument(2)) ? 4L : 0L);
        given(spaceMemberService.getEffectiveMonthlyLimit(eq(MEMBER_ID), eq(SPACE_ID), any(YearMonth.class)))
                .willReturn(4);

        assertThatThrownBy(() -> reservationService.request(SLOT_ID, MEMBER_ID, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(slotMonth.getYear() + "년 " + slotMonth.getMonthValue() + "월")
                .hasMessageContaining("4회");

        then(spaceMemberService).should().getMonthlyUsed(MEMBER_ID, SPACE_ID, slotMonth);
        then(spaceMemberService).should().getEffectiveMonthlyLimit(MEMBER_ID, SPACE_ID, slotMonth);
        then(reservationRepository).should(never()).save(any());
    }
}
