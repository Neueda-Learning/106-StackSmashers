package com.neueda.tms.Common;

import com.neueda.tms.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CommonControllerTests {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void missingResourceIsReturnedAsANotFoundResponse() {
        ResponseEntity<Map<String, Object>> response =
                globalExceptionHandler.handleNotFound(new NoSuchElementException("Alert not found: 42"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody()).containsEntry("error", "Not Found");
        assertThat(response.getBody()).containsEntry("message", "Alert not found: 42");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void conflictingRequestsAreReturnedWithAConflictStatus() {
        ResponseEntity<Map<String, Object>> response =
                globalExceptionHandler.handleBadRequest(new IllegalArgumentException("Transaction already exists"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("status", 409);
        assertThat(response.getBody()).containsEntry("message", "Transaction already exists");
    }

    @Test
    void invalidWorkflowStateIsReturnedAsABadRequest() {
        ResponseEntity<Map<String, Object>> response =
                globalExceptionHandler.handleIllegalState(new IllegalStateException("Alert is already closed."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("message", "Alert is already closed.");
    }

    @Test
    void badCredentialsReturnTheStandardUnauthorizedMessage() {
        ResponseEntity<Map<String, Object>> response =
                globalExceptionHandler.handleBadCredentials(new BadCredentialsException("wrong password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("status", 401);
        assertThat(response.getBody()).containsEntry("message", "Invalid username or password");
    }

    @Test
    void forbiddenOperationsReturnAccessDeniedWithoutLeakingInternalDetails() {
        ResponseEntity<Map<String, Object>> response =
                globalExceptionHandler.handleAccessDenied(new AccessDeniedException("forbidden by policy"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("status", 403);
        assertThat(response.getBody()).containsEntry("message", "Access denied");
    }

    @Test
    void unexpectedErrorsAreWrappedInAGenericInternalServerErrorResponse() {
        ResponseEntity<Map<String, Object>> response =
                globalExceptionHandler.handleGeneral(new RuntimeException("database unavailable"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody()).containsEntry("message", "An unexpected error occurred");
    }

    @Test
    void clientAbortExceptionsAreIgnoredSoTheServerDoesNotFailTheRequestTwice() {
        assertThatCode(() -> globalExceptionHandler.handleClientAbortException(new IOException("broken pipe")))
                .doesNotThrowAnyException();
    }
}
