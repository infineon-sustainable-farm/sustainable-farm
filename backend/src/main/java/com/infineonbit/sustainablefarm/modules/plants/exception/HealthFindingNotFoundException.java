package com.infineonbit.sustainablefarm.modules.plants.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

/** Thrown when no finding of a health inspection has the requested identifier; answered with a 404. */
public class HealthFindingNotFoundException extends ResourceNotFoundException {
    public HealthFindingNotFoundException(Long id) {
        super("Health finding with ID " + id + " not found");
    }
}
