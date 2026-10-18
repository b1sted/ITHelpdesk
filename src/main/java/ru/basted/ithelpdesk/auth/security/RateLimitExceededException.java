package ru.basted.ithelpdesk.auth.security;

public class RateLimitExceededException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RateLimitExceededException(String message) {
        super(message);
    }
}