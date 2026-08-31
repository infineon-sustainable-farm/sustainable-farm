package com.sustainablefarm.core.exception;

/**
 * Categorization of API error responses.
 */
public enum ErrorType {
    VALIDATION_ERROR,
    RESOURCE_NOT_FOUND,
    BUSINESS_RULE_VIOLATION,
    INVALID_STATE_TRANSITION,
    DUPLICATE_RESOURCE,
    CONSTRAINT_VIOLATION,
    BAD_REQUEST,
    INTERNAL_SERVER_ERROR
}
