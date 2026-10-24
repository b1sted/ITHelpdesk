package ru.basted.ithelpdesk.auth.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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

    public void check(String key) {
        long now = Instant.now().getEpochSecond();

        AttemptWindow window = attempts.compute(key, (ignored, existing) ->
                existing == null || isExpired(existing, now)
                        ? new AttemptWindow(now, new AtomicInteger())
                        : existing
        );

        if (window.count().incrementAndGet() > maxAttempts) {
            throw new RateLimitExceededException(
                    "Слишком много попыток входа. Попробуйте позже."
            );
        }
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    @Scheduled(fixedDelayString = "${app.rate-limit.cleanup-ms:60000}")
    public void cleanupExpired() {
        cleanupExpired(Instant.now().getEpochSecond());
    }

    void cleanupExpired(long nowEpochSecond) {
        attempts.entrySet().removeIf(entry -> isExpired(entry.getValue(), nowEpochSecond));
    }

    int trackedKeys() {
        return attempts.size();
    }

    private boolean isExpired(AttemptWindow window, long nowEpochSecond) {
        return nowEpochSecond - window.start() > windowSeconds;
    }

    private record AttemptWindow(long start, AtomicInteger count) {
    }
}
