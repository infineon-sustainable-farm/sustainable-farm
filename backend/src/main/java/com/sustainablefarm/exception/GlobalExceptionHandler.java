package com.sustainablefarm.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

/**
 * Global exception handling for REST API endpoints.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<ApiErrorResponse.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ApiErrorResponse.FieldErrorDetail.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorType.VALIDATION_ERROR,
                "Request validation failed",
                request.getRequestURI(),
                details
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Invalid request";
        ErrorType errorType = resolveBusinessErrorType(message);

        HttpStatus status = errorType == ErrorType.RESOURCE_NOT_FOUND
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;

        return buildResponse(status, errorType, message, request.getRequestURI(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalStateException(
            IllegalStateException ex,
            HttpServletRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Invalid state transition";
        return buildResponse(
                HttpStatus.CONFLICT,
                ErrorType.INVALID_STATE_TRANSITION,
                message,
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        String message = "A database constraint was violated";
        ErrorType errorType = ErrorType.CONSTRAINT_VIOLATION;

        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("duplicate")) {
            errorType = ErrorType.DUPLICATE_RESOURCE;
            message = "A resource with the same unique identifier already exists";
        }

        return buildResponse(
                HttpStatus.CONFLICT,
                errorType,
                message,
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            Exception ex,
            HttpServletRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Malformed request";
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorType.BAD_REQUEST,
                message,
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorType.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request.getRequestURI(),
                null
        );
    }

    private ErrorType resolveBusinessErrorType(String message) {
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("not found")) {
            return ErrorType.RESOURCE_NOT_FOUND;
        }
        if (lowerMessage.contains("cannot advance")
                || lowerMessage.contains("status")
                || lowerMessage.contains("transition")) {
            return ErrorType.INVALID_STATE_TRANSITION;
        }
        if (lowerMessage.contains("already exists") || lowerMessage.contains("duplicate")) {
            return ErrorType.DUPLICATE_RESOURCE;
        }
        return ErrorType.BUSINESS_RULE_VIOLATION;
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            ErrorType errorType,
            String message,
            String path,
            List<ApiErrorResponse.FieldErrorDetail> validationErrors) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(errorType)
                .message(message)
                .path(path)
                .validationErrors(validationErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
