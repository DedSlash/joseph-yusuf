package com.josephyusuf.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PromoCodeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePromoNotFound(PromoCodeNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PromoCodeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handlePromoAlreadyExists(PromoCodeAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PromoCodeException.class)
    public ResponseEntity<ApiErrorResponse> handlePromoException(PromoCodeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Accès refusé : rôle ADMIN requis");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(error -> ((FieldError) error).getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message) {
        ApiErrorResponse error = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .message(message)
                .build();
        return ResponseEntity.status(status).body(error);
    }
}
