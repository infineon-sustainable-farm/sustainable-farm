package com.infineonbit.sustainablefarm.modules.machinery.exception;

public class SparePartNotFoundException extends RuntimeException {
    public SparePartNotFoundException(Long id) {
        super("Spare part not found with id: " + id);
    }
}
