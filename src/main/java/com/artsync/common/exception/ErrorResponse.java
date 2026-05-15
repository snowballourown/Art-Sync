package com.artsync.common.exception;

/**
 * API 오류 응답 공통 포맷.
 */
public record ErrorResponse(int status, String error, String message) {
}
