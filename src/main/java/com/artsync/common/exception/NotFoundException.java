package com.artsync.common.exception;

/**
 * 요청한 리소스(슬롯, 예약, 사용자 등)를 찾을 수 없을 때.
 * 컨트롤러 단계에서 HTTP 404 로 변환된다.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
