package com.example.railgo.service;

import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;

    private static final Duration FAILURE_WINDOW =
            Duration.ofMinutes(10);

    private static final Duration LOCK_DURATION =
            Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Attempt>
            attempts = new ConcurrentHashMap<>();

    public void checkAllowed(String phone) {

        Attempt attempt = attempts.get(phone);

        if (attempt != null
                && attempt.lockedUntil() != null
                && attempt.lockedUntil()
                .isAfter(Instant.now())) {

            throw new BusinessException(
                    ErrorCode.ACCOUNT_LOCKED
            );
        }
    }

    public void recordFailure(String phone) {

        Instant now = Instant.now();

        attempts.compute(phone, (key, old) -> {

            int failures;

            if (old == null
                    || old.windowStartedAt()
                    .plus(FAILURE_WINDOW)
                    .isBefore(now)) {

                failures = 1;

            } else {
                failures = old.failures() + 1;
            }

            Instant windowStart =
                    failures == 1
                            ? now
                            : old.windowStartedAt();

            Instant lockedUntil =
                    failures >= MAX_FAILURES
                            ? now.plus(LOCK_DURATION)
                            : null;

            return new Attempt(
                    failures,
                    windowStart,
                    lockedUntil
            );
        });
    }

    public void recordSuccess(String phone) {
        attempts.remove(phone);
    }

    private record Attempt(
            int failures,
            Instant windowStartedAt,
            Instant lockedUntil
    ) {
    }
}
