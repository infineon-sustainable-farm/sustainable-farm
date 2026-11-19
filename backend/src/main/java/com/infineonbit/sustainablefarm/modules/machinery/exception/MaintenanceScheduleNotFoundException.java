package com.infineonbit.sustainablefarm.modules.machinery.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

public class MaintenanceScheduleNotFoundException extends ResourceNotFoundException {
    public MaintenanceScheduleNotFoundException(Long id) {
        super("Maintenance schedule not found with id: " + id);
    }
}
