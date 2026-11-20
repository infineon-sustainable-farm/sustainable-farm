package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.core.exception.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Scoped to the API-005 controller only; every other error keeps the common
 * envelope of CoreExceptionHandler, which this reuses (ApiError).
 *
 * <p>Two concurrent entries with the same batch_id both pass the existence
 * check; the unique batch code then rejects the second insert. That race must
 * answer 409 like the sequential duplicate, not a generic 500.
 */
@RestControllerAdvice(assignableTypes = StorageEntryController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CropStorageEntryExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(DataIntegrityViolationException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT,
                "Storage entry rejected by a database constraint (most likely an already registered batch_id)",
                request);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleLock(PessimisticLockingFailureException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, "Storage zone busy, retry the storage entry", request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, WebRequest request) {
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), message,
                request.getDescription(false));
        return ResponseEntity.status(status).body(body);
    }
}
