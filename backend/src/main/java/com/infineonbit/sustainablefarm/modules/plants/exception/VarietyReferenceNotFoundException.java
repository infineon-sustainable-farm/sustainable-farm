package com.infineonbit.sustainablefarm.modules.plants.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

/** Unknown variety reference ID. Answered as a 404 ApiError by the core exception handler. */
public class VarietyReferenceNotFoundException extends ResourceNotFoundException {
    public VarietyReferenceNotFoundException(Long id) {
        super("Variety reference with ID " + id + " not found");
    }
}
