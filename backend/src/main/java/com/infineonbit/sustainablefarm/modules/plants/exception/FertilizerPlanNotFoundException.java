package com.infineonbit.sustainablefarm.modules.plants.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

/** Unknown planned application ID. Answered as a 404 ApiError by the core exception handler. */
public class FertilizerPlanNotFoundException extends ResourceNotFoundException {
    public FertilizerPlanNotFoundException(Long id) {
        super("Planned application with ID " + id + " not found");
    }
}
