package com.infineonbit.sustainablefarm.modules.plants.exception;

public class VarietyNotFoundException extends RuntimeException {
    public VarietyNotFoundException(Long id) {
        super("Variety with ID " + id + " not found");
    }
}
