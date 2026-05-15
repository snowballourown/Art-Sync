package com.artsync.config;

import com.artsync.domain.user.UserRepository;
import com.artsync.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 앱 시작 시 기본 사장님(ADMIN) 계정을 만들어 둔다.
 * 회원은 /api/auth/signup 으로 직접 가입하지만, 사장님 계정은 가입 경로가 없으므로
 * 여기서 초기 계정을 보장한다. 이미 존재하면 아무 것도 하지 않는다.
 *
 *   아이디: admin   /   비밀번호: admin1234
 *   (운영 전환 시 반드시 비밀번호를 변경할 것)
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final String DEFAULT_ADMIN_LOGIN_ID = "admin";

    private final UserService userService;
    private final UserRepository userRepository;

    public DataInitializer(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByLoginId(DEFAULT_ADMIN_LOGIN_ID)) {
            userService.registerAdmin(DEFAULT_ADMIN_LOGIN_ID, "admin1234", "화방 사장님", null);
            System.out.println("[DataInitializer] 기본 사장님 계정 생성: admin / admin1234");
        }
    }
}
