// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.bff.security;

import de.warp.bff.application.exception.TooManyLoginAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    private static final Instant WINDOW_START =
            Instant.parse("2026-10-17T10:00:00Z");

    private MutableClock clock;
    private LoginRateLimiter loginRateLimiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(WINDOW_START);

        loginRateLimiter = new LoginRateLimiter(
                clock,
                5,
                Duration.ofSeconds(60)
        );
    }

    @Test
    void shouldAllowFirstFiveLoginAttempts() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThatCode(
                    () -> loginRateLimiter.checkLoginAllowed(
                            "max@example.de",
                            "127.0.0.1"
                    )
            ).doesNotThrowAnyException();
        }
    }

    @Test
    void shouldRejectSixthLoginAttempt() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            loginRateLimiter.checkLoginAllowed(
                    "max@example.de",
                    "127.0.0.1"
            );
        }

        assertThatThrownBy(
                () -> loginRateLimiter.checkLoginAllowed(
                        "max@example.de",
                        "127.0.0.1"
                )
        )
                .isInstanceOf(
                        TooManyLoginAttemptsException.class
                )
                .hasMessage(
                        "Es wurden zu viele Anmeldeversuche "
                                + "durchgeführt. Bitte versuche es "
                                + "später erneut."
                );
    }

    @Test
    void shouldStartNewWindowAfterSixtySeconds() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            loginRateLimiter.checkLoginAllowed(
                    "max@example.de",
                    "127.0.0.1"
            );
        }

        clock.advance(Duration.ofSeconds(60));

        assertThatCode(
                () -> loginRateLimiter.checkLoginAllowed(
                        "max@example.de",
                        "127.0.0.1"
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldNormalizeEmailAddress() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            loginRateLimiter.checkLoginAllowed(
                    " Max@Example.DE ",
                    "127.0.0.1"
            );
        }

        assertThatThrownBy(
                () -> loginRateLimiter.checkLoginAllowed(
                        "max@example.de",
                        "127.0.0.1"
                )
        ).isInstanceOf(
                TooManyLoginAttemptsException.class
        );
    }

    @Test
    void shouldKeepDifferentClientAddressesSeparate() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            loginRateLimiter.checkLoginAllowed(
                    "max@example.de",
                    "127.0.0.1"
            );
        }

        assertThatCode(
                () -> loginRateLimiter.checkLoginAllowed(
                        "max@example.de",
                        "127.0.0.2"
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldKeepDifferentEmailAddressesSeparate() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            loginRateLimiter.checkLoginAllowed(
                    "max@example.de",
                    "127.0.0.1"
            );
        }

        assertThatCode(
                () -> loginRateLimiter.checkLoginAllowed(
                        "erika@example.de",
                        "127.0.0.1"
                )
        ).doesNotThrowAnyException();
    }

    private static final class MutableClock
            extends Clock {

        private Instant currentInstant;

        private MutableClock(Instant currentInstant) {
            this.currentInstant = currentInstant;
        }

        private void advance(Duration duration) {
            currentInstant =
                    currentInstant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(
                java.time.ZoneId zone
        ) {
            return this;
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }
}