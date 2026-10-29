package com.infineonbit.sustainablefarm.modules.machinery.exception;

public class RepairLogNotFoundException extends RuntimeException {
    public RepairLogNotFoundException(Long id) {
        super("Repair log not found with id: " + id);
    }
}
