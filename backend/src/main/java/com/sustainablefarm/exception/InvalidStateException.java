package com.sustainablefarm.exception;

/**
 * Exception thrown when an invalid state transition is attempted
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String message) {
        super(message);
    }

    public InvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
