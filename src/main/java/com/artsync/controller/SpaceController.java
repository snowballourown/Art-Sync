package com.artsync.controller;

import com.artsync.common.web.SessionUtil;
import com.artsync.dto.CreateSpaceRequest;
import com.artsync.dto.IdResponse;
import com.artsync.dto.JoinSpaceRequest;
import com.artsync.dto.SpaceResponse;
import com.artsync.domain.space.Space;
import com.artsync.service.SpaceMemberService;
import com.artsync.service.SpaceService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 공간(Space) CRUD API.
 * 누구든 공간을 만들 수 있다 — 만든 사람이 자동으로 운영자가 된다.
 */
@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

    private final SpaceService spaceService;
    private final SpaceMemberService spaceMemberService;

    public SpaceController(SpaceService spaceService, SpaceMemberService spaceMemberService) {
        this.spaceService = spaceService;
        this.spaceMemberService = spaceMemberService;
    }

    /** 수업(공간) 생성 — 선생님만 */
    @PostMapping
    public IdResponse create(@Valid @RequestBody CreateSpaceRequest request,
                             HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        Long id = spaceService.createSpace(userId, request.name(), request.description());
        return new IdResponse(id);
    }

    /** 학생이 코드로 등록한 수업 목록 */
    @GetMapping
    public List<SpaceResponse> listJoined(HttpSession session) {
        Long memberId = SessionUtil.currentUserId(session);
        return spaceService.getJoinedSpaces(memberId).stream()
                .map(SpaceResponse::of)
                .toList();
    }

    /** 학생이 수업 코드를 입력해 수업에 참여 */
    @PostMapping("/join")
    public SpaceResponse join(@Valid @RequestBody JoinSpaceRequest request, HttpSession session) {
        Long memberId = SessionUtil.currentUserId(session);
        Space space = spaceMemberService.joinByCode(memberId, request.joinCode());
        return SpaceResponse.of(space);
    }

    /** 내가 운영하는 수업 목록 (선생님 전용) */
    @GetMapping("/my")
    public List<SpaceResponse> listMine(HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        return spaceService.getMySpaces(userId).stream()
                .map(SpaceResponse::of)
                .toList();
    }

    /** 수업 단건 조회 */
    @GetMapping("/{id}")
    public SpaceResponse get(@PathVariable Long id) {
        return SpaceResponse.of(spaceService.getSpace(id));
    }

    /** 수업 정보 수정 (선생님 전용) */
    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody CreateSpaceRequest request,
                                       HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        spaceService.updateSpace(userId, id, request.name(), request.description());
        return ResponseEntity.ok().build();
    }

    /** 수업 삭제 (선생님 전용) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpSession session) {
        SessionUtil.requireTeacher(session);
        Long userId = SessionUtil.currentUserId(session);
        spaceService.deleteSpace(userId, id);
        return ResponseEntity.ok().build();
    }
}
