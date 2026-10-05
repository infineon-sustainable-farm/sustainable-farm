package com.infineonbit.sustainablefarm.modules.machinery.exception;

public class EquipmentNotFoundException extends RuntimeException {
    public EquipmentNotFoundException(Long id) {
        super("Equipment with ID " + id + " not found");
    }
}
