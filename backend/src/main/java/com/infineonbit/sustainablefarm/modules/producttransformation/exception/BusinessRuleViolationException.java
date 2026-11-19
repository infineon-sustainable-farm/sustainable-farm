package com.infineonbit.sustainablefarm.modules.producttransformation.exception;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;

/**
 * Exception thrown when a business rule is violated
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public class BusinessRuleViolationException extends BadRequestException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }

    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
