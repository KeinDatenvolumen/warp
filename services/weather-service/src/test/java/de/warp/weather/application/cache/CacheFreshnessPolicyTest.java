// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application.cache;

import de.warp.weather.domain.model.WeatherModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheFreshnessPolicyTest {

    private CacheFreshnessPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new CacheFreshnessPolicy();
    }

    @Test
    void testNullChecks() {
        assertThatThrownBy(() -> policy.normalizeToForecastHour(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> policy.normalizeToQuarterHour(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> policy.ceilToQuarterHour(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> policy.buildCacheKey(49.0, 9.0, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> policy.isExpired(null, Duration.ofMinutes(10), Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void normalizesForecastHour() {
        Instant under30 = Instant.parse("2026-05-16T08:29:00Z");
        assertThat(policy.normalizeToForecastHour(under30))
                .isEqualTo(Instant.parse("2026-05-16T08:00:00Z"));

        Instant exactly30 = Instant.parse("2026-05-16T08:30:00Z");
        assertThat(policy.normalizeToForecastHour(exactly30))
                .isEqualTo(Instant.parse("2026-05-16T09:00:00Z"));

        Instant over30 = Instant.parse("2026-05-16T08:45:00Z");
        assertThat(policy.normalizeToForecastHour(over30))
                .isEqualTo(Instant.parse("2026-05-16T09:00:00Z"));
    }

    @Test
    void normalizesQuarterHour() {
        Instant t1 = Instant.parse("2026-05-16T08:07:00Z");
        assertThat(policy.normalizeToQuarterHour(t1))
                .isEqualTo(Instant.parse("2026-05-16T08:00:00Z"));

        Instant t2 = Instant.parse("2026-05-16T08:08:00Z");
        assertThat(policy.normalizeToQuarterHour(t2))
                .isEqualTo(Instant.parse("2026-05-16T08:15:00Z"));
    }

    @Test
    void ceilsToQuarterHour() {
        Instant exact = Instant.parse("2026-05-16T08:00:00Z");
        assertThat(policy.ceilToQuarterHour(exact)).isEqualTo(exact);

        Instant plusOneSecond = Instant.parse("2026-05-16T08:00:01Z");
        assertThat(policy.ceilToQuarterHour(plusOneSecond))
                .isEqualTo(Instant.parse("2026-05-16T08:15:00Z"));
    }

    @Test
    void roundsCoordinatesAndBuildsDeterministicCacheKey() {
        assertThat(policy.roundCoordinate(49.353612)).isEqualTo(49.3536);
        assertThat(policy.roundCoordinate(9.151189)).isEqualTo(9.1512);

        String key1 = policy.buildCacheKey(49.3536, 9.1512, Instant.parse("2026-05-16T08:10:00Z"), null);
        assertThat(key1).isEqualTo("BEST_MATCH:49.3536:9.1512:2026-05-16T08:00:00Z");

        String key2 = policy.buildCacheKey(49.3536, 9.1512, Instant.parse("2026-05-16T08:10:00Z"), WeatherModel.ICON_SEAMLESS);
        assertThat(key2).isEqualTo("ICON_SEAMLESS:49.3536:9.1512:2026-05-16T08:00:00Z");
    }

    @Test
    void checksExpiration() {
        Instant cachedAt = Instant.parse("2026-05-16T08:00:00Z");
        Duration ttl = Duration.ofMinutes(30);

        Instant beforeTtl = Instant.parse("2026-05-16T08:29:59Z");
        assertThat(policy.isExpired(cachedAt, ttl, beforeTtl)).isFalse();

        Instant afterTtl = Instant.parse("2026-05-16T08:30:01Z");
        assertThat(policy.isExpired(cachedAt, ttl, afterTtl)).isTrue();
    }
}
