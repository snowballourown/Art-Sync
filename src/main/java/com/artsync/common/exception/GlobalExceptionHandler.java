package com.artsync.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 모든 컨트롤러의 예외를 일관된 JSON 포맷으로 변환한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 리소스를 찾을 수 없음 → 404 */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** 비즈니스 규칙 위반(마감/정원/중복/권한 등) → 400 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** 엔터티 상태 전이 위반(이미 처리된 예약 등) → 400 */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleIllegal(RuntimeException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** @Valid 검증 실패 → 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /** 그 외 예상치 못한 오류 → 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleEtc(Exception e) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다: " + e.getMessage());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), message));
    }
}
