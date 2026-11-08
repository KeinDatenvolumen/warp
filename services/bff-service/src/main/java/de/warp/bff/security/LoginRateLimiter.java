// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.security;

import de.warp.bff.application.exception.TooManyLoginAttemptsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot GPT 5.6 Sol) für Sliding-Window Brute-Force-Schutz
@Component
public class LoginRateLimiter {

    private final ConcurrentHashMap<String, AttemptWindow>
            attempts = new ConcurrentHashMap<>();

    private final Clock clock;
    private final int maximumAttempts;
    private final Duration windowDuration;
    private final AtomicLong requestCounter =
            new AtomicLong();

    public LoginRateLimiter(
            Clock clock,
            @Value("${warp.security.login-rate-limit.maximum-attempts:5}")
            int maximumAttempts,
            @Value("${warp.security.login-rate-limit.window:60s}")
            Duration windowDuration
    ) {
        this.clock = clock;
        this.maximumAttempts = maximumAttempts;
        this.windowDuration = windowDuration;
    }

    public void checkLoginAllowed(
            String email,
            String clientAddress
    ) {
        Instant now = clock.instant();

        String key = createKey(
                email,
                clientAddress
        );

        AttemptWindow updatedWindow = attempts.compute(
                key,
                (ignored, existingWindow) ->
                        updateWindow(
                                existingWindow,
                                now
                        )
        );

        if (requestCounter.incrementAndGet() % 100 == 0) {
            removeExpiredEntries(now);
        }

        if (updatedWindow.attempts() > maximumAttempts) {
            throw new TooManyLoginAttemptsException();
        }
    }

    private AttemptWindow updateWindow(
            AttemptWindow existingWindow,
            Instant now
    ) {
        if (existingWindow == null
                || !now.isBefore(
                existingWindow.windowStartedAt()
                        .plus(windowDuration)
        )) {
            return new AttemptWindow(
                    now,
                    1
            );
        }

        return new AttemptWindow(
                existingWindow.windowStartedAt(),
                existingWindow.attempts() + 1
        );
    }

    private String createKey(
            String email,
            String clientAddress
    ) {
        String normalizedEmail = email == null
                ? ""
                : email.trim()
                .toLowerCase(Locale.ROOT);

        String normalizedClientAddress =
                clientAddress == null
                        || clientAddress.isBlank()
                        ? "unknown"
                        : clientAddress.trim();

        return normalizedEmail
                + "|"
                + normalizedClientAddress;
    }

    private void removeExpiredEntries(Instant now) {
        attempts.entrySet().removeIf(entry ->
                !now.isBefore(
                        entry.getValue()
                                .windowStartedAt()
                                .plus(windowDuration)
                )
        );
    }

    private record AttemptWindow(
            Instant windowStartedAt,
            int attempts
    ) {
    }
}