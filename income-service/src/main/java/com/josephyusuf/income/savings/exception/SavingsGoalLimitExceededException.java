package com.josephyusuf.income.savings.exception;

public class SavingsGoalLimitExceededException extends RuntimeException {
    public SavingsGoalLimitExceededException(String message) {
        super(message);
    }
}
