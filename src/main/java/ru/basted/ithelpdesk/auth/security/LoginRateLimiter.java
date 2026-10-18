package ru.basted.ithelpdesk.auth.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {
    private final int maxAttempts;
    private final long windowSeconds;

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${app.rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${app.rate-limit.window-seconds:300}") long windowSeconds
    ) {
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
    }

    public synchronized void check(String key) {
        long now = Instant.now().getEpochSecond();
        AttemptWindow window = attempts.computeIfAbsent(key, k -> new AttemptWindow(now, 0));

        if (now - window.start > windowSeconds) {
            window.start = now;
            window.count = 0;
        }

        window.count++;
        if (window.count > maxAttempts) {
            throw new RateLimitExceededException(
                    "Слишком много попыток входа. Попробуйте позже."
            );
        }
    }

    public synchronized void reset(String key) {
        attempts.remove(key);
    }

    private static final class AttemptWindow {
        private long start;
        private int count;

        private AttemptWindow(long start, int count) {
            this.start = start;
            this.count = count;
        }
    }
}