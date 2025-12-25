package com.graduationCapstone.Probe.global.exception.handler;

import com.graduationCapstone.Probe.global.exception.ErrorResponse;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import jakarta.persistence.EntityNotFoundException;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Service 계층에서 ErrorCode를 담아 던진 모든 비즈니스 예외 처리
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        int status = errorCode.getHttpStatus().value();

        log.warn("Custom Exception [{}]: {}", errorCode.getCode(), e.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                status,
                errorCode.getCode(),
                e.getMessage()
        );

        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }


    /**
     *  @Valid 검증 실패 시 발생 (DTO 유효성 오류)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        // 첫 번째 유효성 검사 실패 메시지 추출
        String message = e.getBindingResult().getAllErrors().getFirst().getDefaultMessage();

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "유효성 검사 실패: " + message
        );
        log.warn("Validation Error: {}", message);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 잘못된 인자 전달, 타입 불일치, 일반적인 IllegalArgumentException 처리
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestException(Exception e) {
        ErrorCode errorCode = ErrorCode.INVALID_ARGUMENT;

        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                e.getMessage()
        );
        log.warn("Bad Request: {}", e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }


    /**
     * DB에서 엔티티를 찾지 못했을 때 (findBy...orElseThrow 등)
     */
    @ExceptionHandler({
            EntityNotFoundException.class,
            NoSuchElementException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFoundException(Exception e) {
        ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage()
        );
        log.warn("Resource Not Found Exception: {}", e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * DB 데이터 무결성 위반 (Unique 제약 조건 위반, FK 위반 등)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        ErrorCode errorCode = ErrorCode.DUPLICATE_RESOURCE;

        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                e.getMessage()
        );
        log.error("Data Integrity Violation: {}", e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT); // 409 Conflict
    }

    /**
     * 인가 실패 (접근 권한 없음)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;

        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                e.getMessage()
        );
        log.warn("Access Denied: {}", e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }


    /**
     * 최종 Fallback 예외 처리 (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception e) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        log.error("Internal Server Error: ", e);

        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}