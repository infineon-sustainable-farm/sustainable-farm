package com.infineonbit.sustainablefarm.core.exception;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Structured error payload returned to API consumers by the global handler.
 *
 * <p>{@code code} is the stable machine-readable form of the status (for example
 * {@code NOT_FOUND}). Clients switch on it, so it is derived from the status
 * instead of being spelled out by each handler. It is kept in sync in the
 * setter too, because deserialization can set the fields in any order.
 */
public class ApiError {

    private Instant timestamp;
    private int status;
    private String code;
    private String error;
    private String message;
    private String path;
    private Map<String, String> fieldErrors;

    public ApiError() {
    }

    public ApiError(int status, String error, String message, String path) {
        this.timestamp = Instant.now();
        this.status = status;
        this.code = HttpStatus.resolve(status) == null
                ? "UNKNOWN"
                : HttpStatus.valueOf(status).name();
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
        this.code = HttpStatus.resolve(status) == null
                ? "UNKNOWN"
                : HttpStatus.valueOf(status).name();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
}
