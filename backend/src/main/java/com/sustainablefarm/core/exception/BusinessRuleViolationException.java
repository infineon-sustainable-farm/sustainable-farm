package com.sustainablefarm.core.exception;

/**
 * Exception thrown when a business rule is violated
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }

    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
