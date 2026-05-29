package com.artsync.service;

import com.artsync.common.exception.BusinessException;
import com.artsync.common.exception.NotFoundException;
import com.artsync.domain.space.Space;
import com.artsync.domain.space.SpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 공간(Space) 생성·조회·권한 검증 서비스.
 * "누가 어느 공간의 운영자인가"를 판단하는 핵심 서비스다.
 */
@Service
@Transactional(readOnly = true)
public class SpaceService {

    private final SpaceRepository spaceRepository;

    public SpaceService(SpaceRepository spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    /** 공간 생성 — 호출자가 자동으로 운영자가 된다. */
    @Transactional
    public Long createSpace(Long ownerId, String name, String description) {
        Space space = new Space(name, description, ownerId);
        return spaceRepository.save(space).getId();
    }

    /** 공간 정보 수정 */
    @Transactional
    public void updateSpace(Long userId, Long spaceId, String name, String description) {
        requireOwner(userId, spaceId);
        getSpace(spaceId).update(name, description);
    }

    /** 공간 삭제 */
    @Transactional
    public void deleteSpace(Long userId, Long spaceId) {
        requireOwner(userId, spaceId);
        spaceRepository.deleteById(spaceId);
    }

    public Space getSpace(Long spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new NotFoundException("공간을 찾을 수 없습니다. id=" + spaceId));
    }

    /** 내가 운영하는 공간 목록 */
    public List<Space> getMySpaces(Long ownerId) {
        return spaceRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    /** 전체 공간 목록 (참가자 탐색용) */
    public List<Space> getAllSpaces() {
        return spaceRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 주어진 사용자가 해당 공간의 운영자인지 검증한다.
     * 아니면 BusinessException 발생.
     */
    public void requireOwner(Long userId, Long spaceId) {
        Space space = getSpace(spaceId);
        if (!space.getOwnerId().equals(userId)) {
            throw new BusinessException("공간 운영자만 사용할 수 있는 기능입니다.");
        }
    }
}
