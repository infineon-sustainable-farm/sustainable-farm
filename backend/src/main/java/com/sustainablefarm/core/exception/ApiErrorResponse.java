package com.sustainablefarm.core.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Structured error response returned by the global exception handler.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final ErrorType error;
    private final String message;
    private final String path;
    private final List<FieldErrorDetail> validationErrors;

    @Getter
    @Builder
    public static class FieldErrorDetail {
        private final String field;
        private final String message;
        private final Object rejectedValue;
    }
}
