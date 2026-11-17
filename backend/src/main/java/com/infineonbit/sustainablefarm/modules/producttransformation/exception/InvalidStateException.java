package com.infineonbit.sustainablefarm.modules.producttransformation.exception;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;

/**
 * Exception thrown when an invalid state transition is attempted
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public class InvalidStateException extends BadRequestException {

    public InvalidStateException(String message) {
        super(message);
    }

    public InvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
