package com.josephyusuf.income.exception;

import com.josephyusuf.income.savings.exception.InvalidSavingsGoalException;
import com.josephyusuf.income.savings.exception.SavingsGoalLimitExceededException;
import com.josephyusuf.income.savings.exception.SavingsGoalNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IncomeSourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(IncomeSourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedAccessException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handlePlanLimit(PlanLimitExceededException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(DuplicateEntryException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateEntryException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(SavingsGoalNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleSavingsNotFound(SavingsGoalNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidSavingsGoalException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSavings(InvalidSavingsGoalException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(SavingsGoalLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleSavingsGoalLimit(SavingsGoalLimitExceededException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
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
