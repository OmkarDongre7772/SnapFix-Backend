package com.snapfix.unit.common.exception;

import com.snapfix.common.exception.ApiError;
import com.snapfix.common.exception.GlobalExceptionHandler;
import com.snapfix.common.exception.ProfileNotFoundException;
import com.snapfix.common.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Handle UserNotFoundException returns 404 and structured error")
    void handleUserNotFound_userNotFound_returns404() {
        // Given
        UserNotFoundException ex = new UserNotFoundException("User id 1 not found");

        // When
        ResponseEntity<ApiError> response = exceptionHandler.handleUserNotFound(ex);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("User id 1 not found");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Handle ProfileNotFoundException returns 404 and structured error")
    void handleProfileNotFound_profileNotFound_returns404() {
        // Given
        ProfileNotFoundException ex = new ProfileNotFoundException("Profile not found for user");

        // When
        ResponseEntity<ApiError> response = exceptionHandler.handleProfileNotFound(ex);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Profile not found for user");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Handle MethodArgumentNotValidException returns 400 and validation map")
    void handleValidationErrors_invalidArgument_returns400WithErrors() throws NoSuchMethodException {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("user", "email", "must be a well-formed email address")
        ));
        
        MethodParameter parameter = mock(MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleValidationErrors(ex);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("error")).isEqualTo("Validation Failed");
        assertThat(body.get("status")).isEqualTo(400);
        
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertThat(errors).containsEntry("email", "must be a well-formed email address");
    }

    @Test
    @DisplayName("Handle IllegalArgumentException returns 400")
    void handleIllegalArgument_invalidArg_returns400() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument provided");

        // When
        ResponseEntity<ApiError> response = exceptionHandler.handleIllegalArgument(ex);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid argument provided");
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Handle Exception returns 500")
    void handleGeneric_unexpectedError_returns500() {
        // Given
        Exception ex = new Exception("Database connection failed");

        // When
        ResponseEntity<ApiError> response = exceptionHandler.handleGeneric(ex);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("Something went wrong: Database connection failed");
        assertThat(response.getBody().getStatus()).isEqualTo(500);
    }
}
