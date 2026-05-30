package com.artsync.service;

import com.artsync.common.exception.BusinessException;
import com.artsync.common.exception.NotFoundException;
import com.artsync.domain.user.Role;
import com.artsync.domain.user.User;
import com.artsync.domain.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 가입/조회 관련 비즈니스 로직.
 * 모든 가입 사용자는 Role.USER 로 동일하며, 공간을 만들면 자동으로 운영자가 된다.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** 회원 가입. 비밀번호는 BCrypt 로 암호화하여 저장한다. 역할은 가입 시 고정된다. */
    @Transactional
    public Long register(String loginId, String rawPassword, String name, String phone, Role role) {
        if (userRepository.existsByLoginId(loginId)) {
            throw new BusinessException("이미 사용 중인 아이디입니다: " + loginId);
        }
        User user = new User(loginId, passwordEncoder.encode(rawPassword), name, phone, role);
        return userRepository.save(user).getId();
    }

    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다. id=" + userId));
    }

    public User getByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다. loginId=" + loginId));
    }

    /** 로그인 검증 — 아이디/비밀번호가 일치하면 User 반환 */
    public User authenticate(String loginId, String rawPassword) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException("아이디 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return user;
    }
}
