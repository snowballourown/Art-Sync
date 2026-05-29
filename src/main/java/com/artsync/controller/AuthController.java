package com.artsync.controller;

import com.artsync.common.web.SessionRole;
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
 * 인증 API.
 *
 * 로그인 시 역할(TEACHER | PARTICIPANT)을 함께 선택한다.
 * 선택한 역할은 세션에 저장되어 이후 요청의 권한 판단에 사용된다.
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
        Long id = userService.register(
                request.loginId(), request.password(), request.name(), request.phone());
        return new IdResponse(id);
    }

    /**
     * 로그인.
     * body: { loginId, password, role: "TEACHER" | "PARTICIPANT" }
     * 성공 시 세션에 userId + sessionRole 저장.
     */
    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        User user = userService.authenticate(request.loginId(), request.password());
        SessionRole role = request.role();
        SessionUtil.login(session, user.getId(), role);
        return UserResponse.from(user, role);
    }

    /** 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        SessionUtil.logout(session);
        return ResponseEntity.ok().build();
    }

    /** 현재 로그인한 사용자 정보 + 세션 역할 */
    @GetMapping("/me")
    public UserResponse me(HttpSession session) {
        Long userId = SessionUtil.currentUserId(session);
        SessionRole role = SessionUtil.currentRole(session);
        return UserResponse.from(userService.getById(userId), role);
    }
}
