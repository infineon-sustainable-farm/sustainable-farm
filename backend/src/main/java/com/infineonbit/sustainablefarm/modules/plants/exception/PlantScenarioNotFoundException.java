package com.infineonbit.sustainablefarm.modules.plants.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

/** Unknown scenario ID. Answered as a 404 ApiError by the core exception handler. */
public class PlantScenarioNotFoundException extends ResourceNotFoundException {
    public PlantScenarioNotFoundException(Long id) {
        super("Scenario with ID " + id + " not found");
    }
}
