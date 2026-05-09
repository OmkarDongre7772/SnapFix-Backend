package com.snapfix.common.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex) {
        ApiError error = new ApiError(
                ex.getMessage(),
                404,
                System.currentTimeMillis());
        return ResponseEntity.status(404).body(error);
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ApiError> handleProfileNotFound(ProfileNotFoundException ex){
        ApiError error = new ApiError(ex.getMessage(), 404, System.currentTimeMillis());
        return ResponseEntity.status(404).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                fieldErrors.put(err.getField(), err.getDefaultMessage()));

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation Failed");
        response.put("status", 400);
        response.put("errors", fieldErrors);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(400).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        ApiError error = new ApiError(ex.getMessage(), 400, System.currentTimeMillis());
        return ResponseEntity.status(400).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
        ApiError error = new ApiError(ex.getMessage(), 409, System.currentTimeMillis());
        return ResponseEntity.status(409).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        System.out.println("DEBUG (GlobalExceptionHandler): Caught an unhandled exception before reaching the endpoint!");
        ex.printStackTrace(); // <-- This will tell us the exact cause!
        ApiError error = new ApiError(
                "Something went wrong: " + ex.getMessage(),
                500,
                System.currentTimeMillis());
        return ResponseEntity.status(500).body(error);
    }
}
