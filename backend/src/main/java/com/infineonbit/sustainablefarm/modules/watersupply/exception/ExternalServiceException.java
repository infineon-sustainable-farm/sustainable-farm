package com.infineonbit.sustainablefarm.modules.watersupply.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** An external service (weather, rainfall, ...) failed: answered 502 by the core exception handler. */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}