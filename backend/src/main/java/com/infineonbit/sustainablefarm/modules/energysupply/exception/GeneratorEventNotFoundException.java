package com.infineonbit.sustainablefarm.modules.energysupply.exception;

public class GeneratorEventNotFoundException extends RuntimeException {

    public GeneratorEventNotFoundException(String eventId) {
        super("Generator event not found: " + eventId);
    }
}
