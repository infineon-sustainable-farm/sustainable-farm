package com.infineonbit.sustainablefarm.modules.plants.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

/** Unknown growth calendar entry ID. Answered as a 404 ApiError by the core exception handler. */
public class GrowthCalendarNotFoundException extends ResourceNotFoundException {
    public GrowthCalendarNotFoundException(Long id) {
        super("Growth calendar entry with ID " + id + " not found");
    }
}
