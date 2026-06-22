package com.artsync.service;

import com.artsync.domain.reservation.ReservationRepository;
import com.artsync.domain.reservation.ReservationStatus;
import com.artsync.domain.spacemember.SpaceMember;
import com.artsync.domain.spacemember.SpaceMemberRepository;
import com.artsync.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SpaceMemberServiceTest {

    @Mock
    private SpaceMemberRepository spaceMemberRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SpaceService spaceService;

    @InjectMocks
    private SpaceMemberService spaceMemberService;

    private static final Long SPACE_ID = 20L;
    private static final Long MEMBER_ID = 30L;

    @Test
    @DisplayName("지정 월 한도는 기본 한도에 직전 달 미사용 횟수를 더한다")
    void getEffectiveMonthlyLimit_includesCarryoverFromPreviousMonth() {
        SpaceMember member = new SpaceMember(SPACE_ID, MEMBER_ID, 4);
        YearMonth targetMonth = YearMonth.now().plusMonths(1);
        YearMonth previousMonth = targetMonth.minusMonths(1);

        given(spaceMemberRepository.findBySpaceIdAndMemberId(SPACE_ID, MEMBER_ID))
                .willReturn(Optional.of(member));
        given(reservationRepository.countMonthlyByMemberAndSpace(
                MEMBER_ID,
                SPACE_ID,
                List.of(ReservationStatus.REQUESTED, ReservationStatus.CONFIRMED),
                previousMonth.getYear(),
                previousMonth.getMonthValue()))
                .willReturn(2L);

        int carryover = spaceMemberService.getCarryover(MEMBER_ID, SPACE_ID, targetMonth);
        int effectiveLimit = spaceMemberService.getEffectiveMonthlyLimit(MEMBER_ID, SPACE_ID, targetMonth);

        assertThat(carryover).isEqualTo(2);
        assertThat(effectiveLimit).isEqualTo(6);
    }
}
