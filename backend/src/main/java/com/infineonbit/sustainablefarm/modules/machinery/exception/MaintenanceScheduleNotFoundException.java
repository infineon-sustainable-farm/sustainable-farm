package com.infineonbit.sustainablefarm.modules.machinery.exception;

public class MaintenanceScheduleNotFoundException extends RuntimeException {
    public MaintenanceScheduleNotFoundException(Long id) {
        super("Maintenance schedule not found with id: " + id);
    }
}
