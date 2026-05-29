package com.artsync.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 보안 설정.
 *
 * 주의: 현재는 개발/학습 단계라 모든 요청을 허용한다.
 * 권한 제어(공간 운영자 여부)는 서비스 계층에서 SpaceService.requireOwner() 로 수행한다.
 *   - /api/spaces/**           → 로그인 사용자 (서비스에서 운영자 여부 추가 검증)
 *   - /api/auth/**             → 모두 허용
 *   - /api/reservations/**     → 로그인 사용자
 *   - /api/notifications/**    → 로그인 사용자
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API + H2 콘솔 사용을 위해 개발 단계에서는 CSRF 비활성화
                .csrf(csrf -> csrf.disable())
                // 개발 단계: 모든 요청 허용 (TODO: 로그인 구현 후 역할별 제어)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // H2 콘솔이 iframe 으로 렌더링되도록 허용
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    /**
     * 비밀번호 암호화에 사용. UserService 에서 주입받아 사용한다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
