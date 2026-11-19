package com.infineonbit.sustainablefarm.modules.machinery.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

public class OperatorAssignmentNotFoundException extends ResourceNotFoundException {
    public OperatorAssignmentNotFoundException(Long id) {
        super("Operator assignment with ID " + id + " not found");
    }
}