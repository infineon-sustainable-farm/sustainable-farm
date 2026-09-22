package com.infineonbit.sustainablefarm.modules.plants.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handling for the plants module only.
 *
 * <p>Scoped with {@code basePackages} on purpose: other modules ship their own
 * advice, and an unscoped advice here would silently capture their exceptions
 * once the feature branches are merged.
 */
@RestControllerAdvice(basePackages = "com.infineonbit.sustainablefarm.modules.plants")
public class PlantsExceptionHandler {

    @ExceptionHandler(VarieteNotFoundException.class)
    public ResponseEntity<String> handleVarieteNotFound(VarieteNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(CalendrierCroissanceNotFoundException.class)
    public ResponseEntity<String> handleCalendrierCroissanceNotFound(CalendrierCroissanceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}
