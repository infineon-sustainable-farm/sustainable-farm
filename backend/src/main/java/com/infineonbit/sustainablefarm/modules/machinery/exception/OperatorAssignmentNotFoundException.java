package com.infineonbit.sustainablefarm.modules.machinery.exception;

public class OperatorAssignmentNotFoundException extends RuntimeException {
    public OperatorAssignmentNotFoundException(Long id) {
        super("Operator assignment with ID " + id + " not found");
    }
}