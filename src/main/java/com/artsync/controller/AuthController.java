package com.artsync.controller;

import com.artsync.common.web.SessionUtil;
import com.artsync.domain.user.User;
import com.artsync.dto.IdResponse;
import com.artsync.dto.LoginRequest;
import com.artsync.dto.SignupRequest;
import com.artsync.dto.UserResponse;
import com.artsync.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API (설계문서 3.1).
 * 회원은 직접 가입하며, 로그인은 세션 기반으로 처리한다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** 회원 가입 */
    @PostMapping("/signup")
    public IdResponse signup(@Valid @RequestBody SignupRequest request) {
        Long id = userService.registerMember(
                request.loginId(), request.password(), request.name(), request.phone());
        return new IdResponse(id);
    }

    /** 로그인 — 성공 시 세션에 사용자 id 저장 */
    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        User user = userService.authenticate(request.loginId(), request.password());
        SessionUtil.login(session, user.getId());
        return UserResponse.from(user);
    }

    /** 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        SessionUtil.logout(session);
        return ResponseEntity.ok().build();
    }

    /** 현재 로그인한 사용자 정보 */
    @GetMapping("/me")
    public UserResponse me(HttpSession session) {
        Long userId = SessionUtil.currentUserId(session);
        return UserResponse.from(userService.getById(userId));
    }
}
