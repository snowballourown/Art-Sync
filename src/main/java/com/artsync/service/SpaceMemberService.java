package com.artsync.service;

import com.artsync.common.exception.NotFoundException;
import com.artsync.common.exception.BusinessException;
import com.artsync.domain.reservation.ReservationRepository;
import com.artsync.domain.reservation.ReservationStatus;
import com.artsync.domain.space.Space;
import com.artsync.domain.spacemember.SpaceMember;
import com.artsync.domain.spacemember.SpaceMemberRepository;
import com.artsync.domain.user.User;
import com.artsync.domain.user.UserRepository;
import com.artsync.dto.SpaceMemberResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 수업 참여자 등록 / 월간 한도 관리 서비스.
 */
@Service
@Transactional(readOnly = true)
public class SpaceMemberService {
    private static final List<ReservationStatus> ACTIVE_RESERVATION_STATUSES =
            List.of(ReservationStatus.REQUESTED, ReservationStatus.CONFIRMED);

    private final SpaceMemberRepository spaceMemberRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final SpaceService spaceService;

    public SpaceMemberService(SpaceMemberRepository spaceMemberRepository,
                              ReservationRepository reservationRepository,
                              UserRepository userRepository,
                              SpaceService spaceService) {
        this.spaceMemberRepository = spaceMemberRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.spaceService = spaceService;
    }

    /**
     * 첫 예약 시 참여자를 수업에 자동 등록.
     * 이미 등록되어 있으면 아무것도 하지 않는다.
     */
    @Transactional
    public SpaceMember getOrCreate(Long spaceId, Long memberId) {
        return spaceMemberRepository.findBySpaceIdAndMemberId(spaceId, memberId)
                .orElseGet(() -> {
                    int defaultLimit = spaceService.getSpace(spaceId).getDefaultMonthlyLimit();
                    return spaceMemberRepository.save(new SpaceMember(spaceId, memberId, defaultLimit));
                });
    }

    /** 참여자가 수업 코드를 입력해 수업에 등록한다. */
    @Transactional
    public Space joinByCode(Long memberId, String joinCode) {
        Space space = spaceService.getSpaceByJoinCode(joinCode);
        getOrCreate(space.getId(), memberId);
        return space;
    }

    public void requireMember(Long spaceId, Long memberId) {
        if (!spaceMemberRepository.existsBySpaceIdAndMemberId(spaceId, memberId)) {
            throw new BusinessException("수업 코드를 먼저 입력해 주세요.");
        }
    }

    /**
     * 선생님용: 수업에 등록된 전체 참여자 목록 + 이번 달 사용 횟수.
     */
    public List<SpaceMemberResponse> getMemberStats(Long userId, Long spaceId) {
        spaceService.requireOwner(userId, spaceId);

        List<SpaceMember> members = spaceMemberRepository.findBySpaceIdOrderByJoinedAtAsc(spaceId);
        if (members.isEmpty()) return List.of();

        YearMonth currentMonth = YearMonth.now();

        // 이번 달 예약 건수 일괄 조회 (memberId → count 맵)
        List<Object[]> rows = reservationRepository.countMonthlyBySpace(
                spaceId,
                ACTIVE_RESERVATION_STATUSES,
                currentMonth.getYear(), currentMonth.getMonthValue());

        Map<Long, Long> usedMap = new HashMap<>();
        for (Object[] row : rows) {
            usedMap.put((Long) row[0], (Long) row[1]);
        }

        // 참여자 이름 일괄 조회
        List<Long> memberIds = members.stream().map(SpaceMember::getMemberId).toList();
        Map<Long, String> nameMap = new HashMap<>();
        userRepository.findAllById(memberIds)
                .forEach(u -> nameMap.put(u.getId(), u.getName()));

        return members.stream()
                .map(sm -> {
                    int carryover = calculateCarryover(sm, currentMonth);
                    return new SpaceMemberResponse(
                            sm.getMemberId(),
                            nameMap.getOrDefault(sm.getMemberId(), "알 수 없음"),
                            sm.getMonthlyLimit(),
                            usedMap.getOrDefault(sm.getMemberId(), 0L),
                            carryover,
                            sm.getMonthlyLimit() + carryover);
                })
                .toList();
    }

    /**
     * 선생님용: 특정 참여자의 월간 한도 수정.
     */
    @Transactional
    public void updateLimit(Long userId, Long spaceId, Long memberId, int monthlyLimit) {
        spaceService.requireOwner(userId, spaceId);
        SpaceMember sm = spaceMemberRepository.findBySpaceIdAndMemberId(spaceId, memberId)
                .orElseThrow(() -> new NotFoundException("해당 수업에 등록된 참여자가 아닙니다."));
        sm.updateMonthlyLimit(monthlyLimit);
    }

    /**
     * 참여자용: 이번 달 해당 수업 사용 횟수 조회.
     */
    public long getMonthlyUsed(Long memberId, Long spaceId) {
        return getMonthlyUsed(memberId, spaceId, YearMonth.now());
    }

    /**
     * 참여자용: 지정한 달의 해당 수업 사용 횟수 조회.
     */
    public long getMonthlyUsed(Long memberId, Long spaceId, YearMonth month) {
        return reservationRepository.countMonthlyByMemberAndSpace(
                memberId, spaceId,
                ACTIVE_RESERVATION_STATUSES,
                month.getYear(), month.getMonthValue());
    }

    /**
     * 참여자의 월간 한도 반환. SpaceMember 가 없으면 space 기본값 사용.
     */
    public int getMonthlyLimit(Long memberId, Long spaceId) {
        return spaceMemberRepository.findBySpaceIdAndMemberId(spaceId, memberId)
                .map(SpaceMember::getMonthlyLimit)
                .orElseGet(() -> spaceService.getSpace(spaceId).getDefaultMonthlyLimit());
    }

    /** 참여자의 이번 달 실제 신청 가능 횟수. */
    public int getEffectiveMonthlyLimit(Long memberId, Long spaceId) {
        return getEffectiveMonthlyLimit(memberId, spaceId, YearMonth.now());
    }

    /** 참여자의 지정 월 실제 신청 가능 횟수: 기본 한도 + 직전 달 미사용 이월분. */
    public int getEffectiveMonthlyLimit(Long memberId, Long spaceId, YearMonth month) {
        int baseLimit = getMonthlyLimit(memberId, spaceId);
        return baseLimit + getCarryover(memberId, spaceId, month);
    }

    /** 직전 달에 남긴 횟수를 이번 달에 더해준다. 이번 달 신규 등록자는 이월 없음. */
    public int getCarryover(Long memberId, Long spaceId) {
        return getCarryover(memberId, spaceId, YearMonth.now());
    }

    /** 지정 월 기준 직전 달 미사용 횟수. */
    public int getCarryover(Long memberId, Long spaceId, YearMonth month) {
        return spaceMemberRepository.findBySpaceIdAndMemberId(spaceId, memberId)
                .map(sm -> calculateCarryover(sm, month))
                .orElse(0);
    }

    private int calculateCarryover(SpaceMember member, YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        if (!member.getJoinedAt().toLocalDate().isBefore(monthStart)) {
            return 0;
        }

        YearMonth previousMonth = month.minusMonths(1);
        long previousUsed = reservationRepository.countMonthlyByMemberAndSpace(
                member.getMemberId(),
                member.getSpaceId(),
                ACTIVE_RESERVATION_STATUSES,
                previousMonth.getYear(),
                previousMonth.getMonthValue());
        return Math.max(0, member.getMonthlyLimit() - (int) previousUsed);
    }
}
