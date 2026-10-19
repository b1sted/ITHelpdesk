package ru.basted.ithelpdesk.auth.security;

import java.io.Serial;

public class RateLimitExceededException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public RateLimitExceededException(String message) {
        super(message);
    }
}