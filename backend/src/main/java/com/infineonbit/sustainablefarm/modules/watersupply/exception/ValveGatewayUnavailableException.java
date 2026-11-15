package com.infineonbit.sustainablefarm.modules.watersupply.exception;

/** The valve command could not be handed to a device gateway (none configured, or gateway down). */
public class ValveGatewayUnavailableException extends RuntimeException {
    public ValveGatewayUnavailableException(String message) {
        super(message);
    }
}
