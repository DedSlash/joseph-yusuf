package com.josephyusuf.auth.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleEmailAlreadyExists - returns 409")
    void handleEmailAlreadyExists() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleEmailAlreadyExists(new EmailAlreadyExistsException("Email taken"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Email taken");
        assertThat(response.getBody().getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("handleBadCredentials - returns 401")
    void handleBadCredentials() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleBadCredentials(new BadCredentialsException("Bad credentials"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).isEqualTo("Bad credentials");
    }

    @Test
    @DisplayName("handleTokenExpired - returns 401")
    void handleTokenExpired() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleTokenExpired(new TokenExpiredException("Token expired"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).isEqualTo("Token expired");
    }

    @Test
    @DisplayName("handleValidation - returns 400 with field errors")
    void handleValidation() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must not be blank"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("email");
    }
}
