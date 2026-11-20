package com.infineonbit.sustainablefarm.modules.watersupply.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The valve command could not be handed to a device gateway (none configured, or gateway down). */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ValveGatewayUnavailableException extends RuntimeException {
    public ValveGatewayUnavailableException(String message) {
        super(message);
    }
}
