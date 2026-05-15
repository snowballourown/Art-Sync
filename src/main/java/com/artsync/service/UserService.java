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
 * (회원이 직접 가입하는 방식 — 설계 진행 시 확정한 가정값)
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

    /** 회원 가입. 비밀번호는 BCrypt 로 암호화하여 저장한다. */
    @Transactional
    public Long registerMember(String loginId, String rawPassword, String name, String phone) {
        if (userRepository.existsByLoginId(loginId)) {
            throw new BusinessException("이미 사용 중인 아이디입니다: " + loginId);
        }
        User user = new User(loginId, passwordEncoder.encode(rawPassword), name, phone, Role.MEMBER);
        return userRepository.save(user).getId();
    }

    /**
     * 사장님 계정 생성. 초기 운영자 계정을 만들 때 사용한다.
     * (회원 가입과 분리 — 사장님은 시스템에 1명만 존재한다고 가정)
     */
    @Transactional
    public Long registerAdmin(String loginId, String rawPassword, String name, String phone) {
        if (userRepository.existsByLoginId(loginId)) {
            throw new BusinessException("이미 사용 중인 아이디입니다: " + loginId);
        }
        User user = new User(loginId, passwordEncoder.encode(rawPassword), name, phone, Role.ADMIN);
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

    /** 로그인 검증에 사용 — 아이디/비밀번호가 일치하면 User 반환 */
    public User authenticate(String loginId, String rawPassword) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException("아이디 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return user;
    }

    /** 사장님 권한 확인 — 권한이 필요한 작업 앞에서 호출 */
    public void requireAdmin(Long userId) {
        if (!getById(userId).isAdmin()) {
            throw new BusinessException("사장님만 사용할 수 있는 기능입니다.");
        }
    }
}
