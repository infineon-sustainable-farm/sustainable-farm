package com.infineonbit.sustainablefarm.modules.machinery.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

public class EquipmentNotFoundException extends ResourceNotFoundException {
    public EquipmentNotFoundException(Long id) {
        super("Equipment with ID " + id + " not found");
    }
}
