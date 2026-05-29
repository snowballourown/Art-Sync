package com.artsync.config;

import com.artsync.domain.user.UserRepository;
import com.artsync.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 앱 시작 시 실행되는 초기화 로직.
 * Space 기반 구조에서는 별도 관리자 계정이 필요 없으므로
 * 개발용 테스트 계정만 최초 1회 생성한다.
 * (운영 환경에서는 이 클래스를 비활성화하거나 제거할 것)
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final String DEV_USER_LOGIN_ID = "test";

    private final UserService userService;
    private final UserRepository userRepository;

    public DataInitializer(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByLoginId(DEV_USER_LOGIN_ID)) {
            userService.register(DEV_USER_LOGIN_ID, "test1234", "테스트 사용자", null);
            System.out.println("[DataInitializer] 개발용 계정 생성: test / test1234");
        }
    }
}
