package com.josephyusuf.income.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleNotFound - returns 404")
    void handleNotFound() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleNotFound(new IncomeSourceNotFoundException("Not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Not found");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("handleUnauthorized - returns 403")
    void handleUnauthorized() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleUnauthorized(new UnauthorizedAccessException("Forbidden"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).isEqualTo("Forbidden");
    }

    @Test
    @DisplayName("handlePlanLimit - returns 403")
    void handlePlanLimit() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handlePlanLimit(new PlanLimitExceededException("Limit reached"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).isEqualTo("Limit reached");
    }

    @Test
    @DisplayName("handleDuplicate - returns 409")
    void handleDuplicate() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleDuplicate(new DuplicateEntryException("Duplicate"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Duplicate");
    }

    @Test
    @DisplayName("handleValidation - returns 400 with field error details")
    void handleValidation() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "amount", "must not be null"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("amount: must not be null");
    }
}
