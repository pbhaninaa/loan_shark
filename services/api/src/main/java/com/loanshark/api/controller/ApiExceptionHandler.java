package com.loanshark.api.controller;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = fieldErrors(exception.getBindingResult().getFieldErrors());
        return ResponseEntity.badRequest().body(error(validationMessage(fieldErrors), fieldErrors));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBind(BindException exception) {
        Map<String, String> fieldErrors = fieldErrors(exception.getBindingResult().getFieldErrors());
        return ResponseEntity.badRequest().body(error(validationMessage(fieldErrors), fieldErrors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
            .body(error(exception.getReason() == null ? "Request failed" : exception.getReason(), null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(error(exception.getMessage(), null));
    }

    private Map<String, Object> error(String message, Map<String, String> details) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("message", message);
        if (details != null) {
            body.put("details", details);
        }
        return body;
    }

    private Map<String, String> fieldErrors(java.util.List<FieldError> errors) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : errors) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return fieldErrors;
    }

    private String validationMessage(Map<String, String> details) {
        if (details.isEmpty()) {
            return "Validation failed";
        }
        return details.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .reduce("Validation failed", (left, right) -> left.equals("Validation failed") ? right : left + "; " + right);
    }
}
