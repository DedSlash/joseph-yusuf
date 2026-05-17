package com.josephyusuf.support.exception;

public class TicketAccessDeniedException extends RuntimeException {
    public TicketAccessDeniedException(String message) {
        super(message);
    }
}
