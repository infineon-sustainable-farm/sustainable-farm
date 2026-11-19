package com.infineonbit.sustainablefarm.modules.watersupply.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

/**
 * Exception raised when a requested resource does not exist.
 * Answered 404 by the core exception handler.
 */
public class NotFoundException extends ResourceNotFoundException {
    public NotFoundException(String resource) {
        super(resource + " not found");
    }
}
