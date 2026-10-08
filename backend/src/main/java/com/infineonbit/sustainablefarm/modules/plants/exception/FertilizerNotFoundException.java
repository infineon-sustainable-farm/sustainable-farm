package com.infineonbit.sustainablefarm.modules.plants.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

/** Unknown fertilizer ID. Answered as a 404 ApiError by the core exception handler. */
public class FertilizerNotFoundException extends ResourceNotFoundException {
    public FertilizerNotFoundException(Long id) {
        super("Fertilizer with ID " + id + " not found");
    }
}
