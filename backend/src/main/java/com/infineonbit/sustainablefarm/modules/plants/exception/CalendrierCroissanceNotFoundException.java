package com.infineonbit.sustainablefarm.modules.plants.exception;

public class CalendrierCroissanceNotFoundException extends RuntimeException {
    public CalendrierCroissanceNotFoundException(Long id) {
        super("Growth calendar entry with ID " + id + " not found");
    }
}
