package com.infineonbit.sustainablefarm.modules.plants.exception;

public class VarieteNotFoundException extends RuntimeException {
    public VarieteNotFoundException(Long id) {
        super("Variety with ID " + id + " not found");
    }
}
