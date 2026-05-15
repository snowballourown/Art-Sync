package com.artsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 화방 예약 관리 시스템 (Art-Sync) 진입점.
 *
 * 실행: ./gradlew bootRun
 * 기본 프로필은 local (H2 인메모리 DB) 입니다.
 */
@SpringBootApplication
public class ArtSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArtSyncApplication.class, args);
    }
}
