package com.artsync.controller;

import com.artsync.common.web.SessionRole;
import com.artsync.common.web.SessionUtil;
import com.artsync.domain.user.Role;
import com.artsync.domain.user.User;
import com.artsync.dto.FindLoginIdRequest;
import com.artsync.dto.FindLoginIdResponse;
import com.artsync.dto.IdResponse;
import com.artsync.dto.LoginRequest;
import com.artsync.dto.ResetPasswordRequest;
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
 * 회원가입 시 역할(TEACHER | PARTICIPANT)을 선택하고 DB에 저장한다.
 * 로그인은 아이디/비밀번호만 입력하면 저장된 역할이 자동으로 세션에 설정된다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** 회원 가입 — 역할(TEACHER | PARTICIPANT)을 선택하여 DB에 저장 */
    @PostMapping("/signup")
    public IdResponse signup(@Valid @RequestBody SignupRequest request) {
        Long id = userService.register(
                request.loginId(), request.password(), request.name(), request.phone(),
                request.securityQuestion(), request.securityAnswer(), request.role());
        return new IdResponse(id);
    }

    /**
     * 로그인.
     * body: { loginId, password }
     * 역할은 가입 시 저장된 값을 자동으로 사용. 성공 시 세션에 userId + sessionRole 저장.
     */
    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        User user = userService.authenticate(request.loginId(), request.password());
        SessionRole role = user.getRole() == Role.TEACHER ? SessionRole.TEACHER : SessionRole.PARTICIPANT;
        SessionUtil.login(session, user.getId(), role);
        return UserResponse.from(user, role);
    }

    /** 아이디 찾기 — 가입 시 입력한 이름과 연락처로 로그인 아이디를 찾는다. */
    @PostMapping("/find-login-id")
    public FindLoginIdResponse findLoginId(@Valid @RequestBody FindLoginIdRequest request) {
        return new FindLoginIdResponse(userService.findLoginIds(
                request.name(), request.phone(), request.securityQuestion(), request.securityAnswer()));
    }

    /** 비밀번호 재설정 — 본인 정보 확인 후 새 비밀번호로 변경한다. */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(
                request.loginId(), request.name(), request.phone(),
                request.securityQuestion(), request.securityAnswer(), request.newPassword());
        return ResponseEntity.ok().build();
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
