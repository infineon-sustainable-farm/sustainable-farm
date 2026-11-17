package com.infineonbit.sustainablefarm.modules.machinery.exception;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;

public class EquipmentAlreadyAssignedException extends ConflictException {
    public EquipmentAlreadyAssignedException(Long equipmentId) {
        super("Equipment with ID " + equipmentId + " is already assigned to an operator.");
    }
}