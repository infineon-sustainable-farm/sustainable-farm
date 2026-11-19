package com.infineonbit.sustainablefarm.modules.plants.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

/** Unknown harvest ID. Answered as a 404 ApiError by the core exception handler. */
public class HarvestNotFoundException extends ResourceNotFoundException {
    public HarvestNotFoundException(Long id) {
        super("Harvest with ID " + id + " not found");
    }
}
