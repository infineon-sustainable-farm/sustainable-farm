package com.infineonbit.sustainablefarm.modules.machinery.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

public class FuelLogNotFoundException extends ResourceNotFoundException {
    public FuelLogNotFoundException(Long id) {
        super("Fuel log not found with id: " + id);
    }
}
