package com.infineonbit.sustainablefarm.core.exception;

/**
 * Thrown when an operation is attempted in a state that does not allow it
 * (HTTP 422 / 409). Example: check-in a registration that was not confirmed.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
