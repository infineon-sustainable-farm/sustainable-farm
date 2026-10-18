package com.infineonbit.sustainablefarm.modules.plants.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

/** Unknown variety ID. Answered as a 404 ApiError by the core exception handler. */
public class VarietyNotFoundException extends ResourceNotFoundException {
    public VarietyNotFoundException(Long id) {
        super("Variety with ID " + id + " not found");
    }
}
