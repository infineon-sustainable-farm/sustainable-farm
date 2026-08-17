package com.sustainablefarm.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/batches");
    }

    @Test
    void handleValidationErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "batchId", "", false, null, null, "Batch ID is required"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidationErrors(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorType.VALIDATION_ERROR, response.getBody().getError());
        assertEquals(1, response.getBody().getValidationErrors().size());
        assertEquals("batchId", response.getBody().getValidationErrors().get(0).getField());
    }

    @Test
    void handleResourceNotFound() {
        IllegalArgumentException ex = new IllegalArgumentException("Batch not found with ID: X");

        ResponseEntity<ApiErrorResponse> response = handler.handleIllegalArgumentException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ErrorType.RESOURCE_NOT_FOUND, response.getBody().getError());
    }

    @Test
    void handleBusinessRuleViolation() {
        IllegalArgumentException ex = new IllegalArgumentException("Lot code is mandatory for traceability compliance");

        ResponseEntity<ApiErrorResponse> response = handler.handleIllegalArgumentException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorType.BUSINESS_RULE_VIOLATION, response.getBody().getError());
    }

    @Test
    void handleInvalidStateTransition() {
        IllegalStateException ex = new IllegalStateException("Cannot advance status from SHIPPED");

        ResponseEntity<ApiErrorResponse> response = handler.handleIllegalStateException(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(ErrorType.INVALID_STATE_TRANSITION, response.getBody().getError());
    }

    @Test
    void handleDataIntegrityViolation() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("duplicate key value");

        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(ErrorType.DUPLICATE_RESOURCE, response.getBody().getError());
    }

    @Test
    void handleUnexpectedException() {
        Exception ex = new RuntimeException("database connection failed");

        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpectedException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ErrorType.INTERNAL_SERVER_ERROR, response.getBody().getError());
        assertFalse(response.getBody().getMessage().contains("database"));
    }
}
