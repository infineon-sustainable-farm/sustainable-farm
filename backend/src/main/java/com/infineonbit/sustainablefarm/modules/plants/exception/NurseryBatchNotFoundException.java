package com.infineonbit.sustainablefarm.modules.plants.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

/** Unknown nursery batch ID. Answered as a 404 ApiError by the core exception handler. */
public class NurseryBatchNotFoundException extends ResourceNotFoundException {
    public NurseryBatchNotFoundException(Long id) {
        super("Nursery batch with ID " + id + " not found");
    }
}
