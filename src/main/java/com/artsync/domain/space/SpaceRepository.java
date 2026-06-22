package com.artsync.domain.space;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpaceRepository extends JpaRepository<Space, Long> {

    /** 내가 운영하는 공간 목록 */
    List<Space> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<Space> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);

    /** 참여자가 등록한 공간 목록 */
    @Query("""
            select s
            from Space s
            join SpaceMember sm on sm.spaceId = s.id
            where sm.memberId = :memberId
            order by sm.joinedAt desc
            """)
    List<Space> findJoinedByMemberId(@Param("memberId") Long memberId);

    /** 전체 공간 목록 (참가자용 탐색) */
    List<Space> findAllByOrderByCreatedAtDesc();
}
