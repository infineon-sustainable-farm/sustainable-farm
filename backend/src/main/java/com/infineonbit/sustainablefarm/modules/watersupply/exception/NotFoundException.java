package com.infineonbit.sustainablefarm.modules.watersupply.exception;

/**
 * Exception levee quand une ressource demandee n'existe pas.
 * Le code 404 est gere par GlobalExceptionHandler.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String resource) {
        super(resource + " not found");
    }
}
