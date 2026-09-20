package com.infineonbit.sustainablefarm.modules.machinery.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice(basePackages = "com.infineonbit.sustainablefarm.modules.machinery")
public class GlobalExceptionHandler{
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolationException(DataIntegrityViolationException e){
        String message = e.getMostSpecificCause().getMessage();
        if (message != null && (message.contains("duplicate key") || message.contains("Duplicate entry")
                || message.contains("unique constraint") || message.contains("UNIQUE constraint"))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("A record with the same unique value already exists.");
        }
        if (message != null && message.contains("foreign key")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("This operation conflicts with a related record. Please refresh and try again.");
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("The request conflicts with the current state of the data. Please refresh and try again.");
    }

    @ExceptionHandler (MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleInvalidMethodException(MethodArgumentNotValidException e){
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EquipmentNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EquipmentNotFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(SparePartNotFoundException.class)
    public ResponseEntity<String> handleSparePartNotFound(SparePartNotFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(EquipmentAlreadyAssignedException.class)
    public ResponseEntity<String> handleEquipmentAlreadyAssigned(EquipmentAlreadyAssignedException e){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(OperatorAssignmentNotFoundException.class)
    public ResponseEntity<String> handleOperatorAssignmentNotFound(OperatorAssignmentNotFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<String> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException e){
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("This record was modified or deleted by another user. Please refresh and try again.");
    }

}
