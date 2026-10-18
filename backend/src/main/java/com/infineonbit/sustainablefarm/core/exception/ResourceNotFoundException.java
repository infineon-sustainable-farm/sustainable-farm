package com.infineonbit.sustainablefarm.core.exception;

/**
 * Thrown when a requested resource cannot be found (HTTP 404).
 * Dress as a RuntimeException so it can bubble through Spring layers
 * and be mapped by the global exception handler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
