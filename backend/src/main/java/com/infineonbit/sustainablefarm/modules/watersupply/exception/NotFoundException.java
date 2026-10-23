package com.infineonbit.sustainablefarm.modules.watersupply.exception;

/**
 * Exception raised when a requested resource does not exist.
 * The 404 code is handled by GlobalExceptionHandler.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String resource) {
        super(resource + " not found");
    }
}
