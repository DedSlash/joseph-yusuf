package com.josephyusuf.support.exception;

public class TicketLimitExceededException extends RuntimeException {
    public TicketLimitExceededException(String message) {
        super(message);
    }
}
