package ru.basted.ithelpdesk.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {
    @Test
    void removesExpiredAttemptWindows() {
        LoginRateLimiter limiter = new LoginRateLimiter(5, 300);
        limiter.check("login:user@company.ru");

        assertEquals(1, limiter.trackedKeys());

        limiter.cleanupExpired(Instant.now().getEpochSecond() + 301);

        assertEquals(0, limiter.trackedKeys());
    }

    @Test
    void keepsActiveAttemptWindows() {
        LoginRateLimiter limiter = new LoginRateLimiter(5, 300);
        limiter.check("login:user@company.ru");

        limiter.cleanupExpired(Instant.now().getEpochSecond());

        assertEquals(1, limiter.trackedKeys());
    }

    @Test
    void blocksAttemptsAboveConfiguredLimit() {
        LoginRateLimiter limiter = new LoginRateLimiter(2, 300);

        limiter.check("login:user@company.ru");
        limiter.check("login:user@company.ru");

        assertThrows(
                RateLimitExceededException.class,
                () -> limiter.check("login:user@company.ru")
        );
    }

    @Test
    void resetRemovesWindow() {
        LoginRateLimiter limiter = new LoginRateLimiter(5, 300);
        limiter.check("login:user@company.ru");

        limiter.reset("login:user@company.ru");

        assertEquals(0, limiter.trackedKeys());
    }
}
