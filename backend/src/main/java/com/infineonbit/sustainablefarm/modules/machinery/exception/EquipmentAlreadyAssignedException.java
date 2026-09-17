package com.infineonbit.sustainablefarm.modules.machinery.exception;

public class EquipmentAlreadyAssignedException extends RuntimeException {
    public EquipmentAlreadyAssignedException(Long equipmentId) {
        super("Equipment with ID " + equipmentId + " is already assigned to an operator.");
    }
}