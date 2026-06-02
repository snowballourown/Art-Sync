package com.artsync.domain.spacemember;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpaceMemberRepository extends JpaRepository<SpaceMember, Long> {

    Optional<SpaceMember> findBySpaceIdAndMemberId(Long spaceId, Long memberId);

    List<SpaceMember> findBySpaceIdOrderByJoinedAtAsc(Long spaceId);

    boolean existsBySpaceIdAndMemberId(Long spaceId, Long memberId);
}
