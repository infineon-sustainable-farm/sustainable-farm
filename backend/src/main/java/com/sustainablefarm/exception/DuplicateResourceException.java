package com.sustainablefarm.exception;

/**
 * Exception thrown when attempting to create a duplicate resource
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
