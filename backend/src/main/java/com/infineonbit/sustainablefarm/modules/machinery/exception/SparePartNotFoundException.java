package com.infineonbit.sustainablefarm.modules.machinery.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

public class SparePartNotFoundException extends ResourceNotFoundException {
    public SparePartNotFoundException(Long id) {
        super("Spare part not found with id: " + id);
    }
}
