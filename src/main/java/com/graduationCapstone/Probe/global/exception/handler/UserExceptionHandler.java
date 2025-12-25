package com.graduationCapstone.Probe.global.exception.handler;

import com.graduationCapstone.Probe.global.exception.ErrorResponse;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("Custom Exception [{}]: {}", errorCode.getCode(), e.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                e.getMessage()
        );

        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorCode errorCode = ErrorCode.INVALID_ARGUMENT;

        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                e.getMessage()
        );
        log.warn("IllegalArgumentException: {}", e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}