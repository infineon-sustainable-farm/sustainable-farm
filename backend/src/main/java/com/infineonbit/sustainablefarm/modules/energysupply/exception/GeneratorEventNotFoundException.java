package com.infineonbit.sustainablefarm.modules.energysupply.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

public class GeneratorEventNotFoundException extends ResourceNotFoundException {

    public GeneratorEventNotFoundException(String eventId) {
        super("Generator event not found: " + eventId);
    }
}
