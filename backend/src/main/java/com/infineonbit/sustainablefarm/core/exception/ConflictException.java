package com.infineonbit.sustainablefarm.core.exception;

/**
 * Thrown when a request violates a business rule that must not allow
 * duplicate or conflicting state (HTTP 409).
 *
 * <p>Examples: duplicate visitor email, two time slots overlapping,
 * registration filling the same slot twice.</p>
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
