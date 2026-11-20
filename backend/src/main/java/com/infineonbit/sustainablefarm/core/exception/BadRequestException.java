package com.infineonbit.sustainablefarm.core.exception;

/**
 * Thrown when a request is refused as invalid by a rule of the domain
 * (HTTP 400), for example a state transition that is not allowed.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
