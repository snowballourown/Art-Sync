package com.artsync.domain.space;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceRepository extends JpaRepository<Space, Long> {

    /** 내가 운영하는 공간 목록 */
    List<Space> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    /** 전체 공간 목록 (참가자용 탐색) */
    List<Space> findAllByOrderByCreatedAtDesc();
}
