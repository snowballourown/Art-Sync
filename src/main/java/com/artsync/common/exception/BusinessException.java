package com.artsync.common.exception;

/**
 * 비즈니스 규칙 위반 (예약 마감, 정원 초과, 중복 요청 등).
 * 컨트롤러 단계에서 HTTP 400 으로 변환된다.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
