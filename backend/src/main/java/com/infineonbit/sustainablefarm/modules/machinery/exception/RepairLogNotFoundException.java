package com.infineonbit.sustainablefarm.modules.machinery.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

public class RepairLogNotFoundException extends ResourceNotFoundException {
    public RepairLogNotFoundException(Long id) {
        super("Repair log not found with id: " + id);
    }
}
