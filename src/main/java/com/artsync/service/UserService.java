package com.artsync.service;

import com.artsync.common.exception.BusinessException;
import com.artsync.common.exception.NotFoundException;
import com.artsync.domain.user.Role;
import com.artsync.domain.user.SecurityQuestion;
import com.artsync.domain.user.User;
import com.artsync.domain.user.UserRepository;
import java.util.List;
import java.util.Locale;
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
    public Long register(String loginId, String rawPassword, String name, String phone,
                         String securityQuestion, String securityAnswer, Role role) {
        String normalizedLoginId = normalizeText(loginId);
        String normalizedPhone = normalizePhone(phone);
        String normalizedQuestion = normalizeQuestion(securityQuestion);
        String normalizedAnswer = normalizeAnswer(securityAnswer);
        validateRecoveryInputs(normalizedQuestion, normalizedAnswer);
        if (normalizedPhone.isEmpty()) {
            throw new BusinessException("연락처를 입력해 주세요.");
        }
        if (userRepository.existsByLoginId(normalizedLoginId)) {
            throw new BusinessException("이미 사용 중인 아이디입니다: " + normalizedLoginId);
        }
        User user = new User(
                normalizedLoginId,
                passwordEncoder.encode(rawPassword),
                normalizeText(name),
                normalizedPhone,
                normalizedQuestion,
                passwordEncoder.encode(normalizedAnswer),
                role);
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

    /** 이름과 연락처가 일치하는 계정의 로그인 아이디를 찾는다. */
    public List<String> findLoginIds(String name, String phone,
                                     String securityQuestion, String securityAnswer) {
        String normalizedName = normalizeText(name);
        String normalizedPhone = normalizePhone(phone);
        String normalizedQuestion = normalizeQuestion(securityQuestion);
        String normalizedAnswer = normalizeAnswer(securityAnswer);
        validateRecoveryInputs(normalizedQuestion, normalizedAnswer);
        if (normalizedName.isEmpty() || normalizedPhone.isEmpty()) {
            throw new BusinessException("이름과 연락처를 입력해 주세요.");
        }

        List<String> loginIds = userRepository.findByName(normalizedName).stream()
                .filter(user -> phoneMatches(user.getPhone(), normalizedPhone))
                .filter(user -> securityAnswerMatches(user, normalizedQuestion, normalizedAnswer))
                .map(User::getLoginId)
                .sorted()
                .toList();

        if (loginIds.isEmpty()) {
            throw new BusinessException("입력한 정보와 일치하는 아이디가 없습니다.");
        }
        return loginIds;
    }

    /** 본인 정보가 일치하면 새 비밀번호로 재설정한다. */
    @Transactional
    public void resetPassword(String loginId, String name, String phone,
                              String securityQuestion, String securityAnswer, String newPassword) {
        String normalizedLoginId = normalizeText(loginId);
        String normalizedName = normalizeText(name);
        String normalizedPhone = normalizePhone(phone);
        String normalizedQuestion = normalizeQuestion(securityQuestion);
        String normalizedAnswer = normalizeAnswer(securityAnswer);
        validateRecoveryInputs(normalizedQuestion, normalizedAnswer);
        if (normalizedLoginId.isEmpty() || normalizedName.isEmpty() || normalizedPhone.isEmpty()) {
            throw new BusinessException("아이디, 이름, 연락처를 모두 입력해 주세요.");
        }

        User user = userRepository.findByLoginId(normalizedLoginId)
                .orElseThrow(() -> new BusinessException("입력한 정보와 일치하는 계정을 찾을 수 없습니다."));

        if (!normalizeText(user.getName()).equals(normalizedName)
                || !phoneMatches(user.getPhone(), normalizedPhone)
                || !securityAnswerMatches(user, normalizedQuestion, normalizedAnswer)) {
            throw new BusinessException("입력한 정보와 일치하는 계정을 찾을 수 없습니다.");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
    }

    private boolean phoneMatches(String savedPhone, String normalizedRequestPhone) {
        return !normalizedRequestPhone.isEmpty() && normalizePhone(savedPhone).equals(normalizedRequestPhone);
    }

    private boolean securityAnswerMatches(User user, String normalizedQuestion, String normalizedAnswer) {
        return hasText(user.getSecurityAnswer())
                && normalizedQuestion.equals(user.getSecurityQuestion())
                && passwordEncoder.matches(normalizedAnswer, user.getSecurityAnswer());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizePhone(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    private String normalizeAnswer(String value) {
        return normalizeText(value).replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private String normalizeQuestion(String value) {
        String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "";
        }
        try {
            return SecurityQuestion.valueOf(normalized).name();
        } catch (IllegalArgumentException e) {
            throw new BusinessException("복구 질문을 목록에서 선택해 주세요.");
        }
    }

    private void validateRecoveryInputs(String securityQuestion, String securityAnswer) {
        if (securityQuestion.isEmpty() || securityAnswer.isEmpty()) {
            throw new BusinessException("복구 질문과 답변을 모두 입력해 주세요.");
        }
    }
}
