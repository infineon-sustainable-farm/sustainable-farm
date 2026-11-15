package com.infineonbit.sustainablefarm.modules.machinery.exception;

public class FuelLogNotFoundException extends RuntimeException {
    public FuelLogNotFoundException(Long id) {
        super("Fuel log not found with id: " + id);
    }
}
