// [KI-Log Nr. KI-22] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): In-Memory-Caching mit Caffeine (WARP-035)
package de.warp.identity.service;

import de.warp.identity.entity.SessionEntity;
import de.warp.identity.entity.UserEntity;
import de.warp.identity.exceptions.SessionNotFoundException;
import de.warp.identity.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CachedSessionServiceTest {

    private AnnotationConfigApplicationContext context;
    private SessionRepository sessionRepository;
    private CachedSessionService cachedSessionService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(
                TestConfiguration.class
        );

        sessionRepository = context.getBean(SessionRepository.class);
        cachedSessionService = context.getBean(CachedSessionService.class);

        CacheManager cacheManager = context.getBean(CacheManager.class);
        var cache = cacheManager.getCache(CachedSessionService.SESSION_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void cachesSuccessfulSessionLookup() {
        String tokenHash = "a".repeat(64);
        SessionEntity session = createSession(tokenHash);

        when(sessionRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(session));

        CachedSession firstResult =
                cachedSessionService.findByTokenHash(tokenHash);
        CachedSession secondResult =
                cachedSessionService.findByTokenHash(tokenHash);

        assertThat(firstResult).isEqualTo(secondResult);
        assertThat(firstResult.id()).isEqualTo("ses_123");
        assertThat(firstResult.userId()).isEqualTo("usr_123");
        assertThat(firstResult.emailVerified()).isTrue();

        verify(sessionRepository, times(1))
                .findByTokenHash(tokenHash);
    }

    @Test
    void evictsSessionFromCache() {
        String tokenHash = "b".repeat(64);
        SessionEntity session = createSession(tokenHash);

        when(sessionRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(session));

        cachedSessionService.findByTokenHash(tokenHash);
        cachedSessionService.evictByTokenHash(tokenHash);
        cachedSessionService.findByTokenHash(tokenHash);

        verify(sessionRepository, times(2))
                .findByTokenHash(tokenHash);
    }

    @Test
    void doesNotCacheExceptions() {
        String tokenHash = "c".repeat(64);
        SessionEntity session = createSession(tokenHash);

        when(sessionRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(session));

        assertThatThrownBy(
                () -> cachedSessionService.findByTokenHash(tokenHash)
        ).isInstanceOf(SessionNotFoundException.class);

        CachedSession result =
                cachedSessionService.findByTokenHash(tokenHash);

        assertThat(result.id()).isEqualTo("ses_123");

        verify(sessionRepository, times(2))
                .findByTokenHash(tokenHash);
    }

    @Test
    void cachedSessionLookupMeetsSubFiveMillisecondResponseTimeTarget() {
        String tokenHash = "d".repeat(64);
        SessionEntity session = createSession(tokenHash);

        when(sessionRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(session));

        // Warm up cache (first lookup hits repository)
        cachedSessionService.findByTokenHash(tokenHash);

        // Measure 100 consecutive cached lookups (NFR-P-01: < 5 ms per cached lookup)
        long startNanos = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            CachedSession cached = cachedSessionService.findByTokenHash(tokenHash);
            assertThat(cached.userId()).isEqualTo("usr_123");
        }
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
        double avgMillisPerLookup = elapsedMillis / 100.0;

        assertThat(avgMillisPerLookup).isLessThan(5.0);
        verify(sessionRepository, times(1)).findByTokenHash(tokenHash);
    }

    private SessionEntity createSession(String tokenHash) {
        UserEntity user = new UserEntity(
                "usr_123",
                "Max Mustermann",
                "max@example.de",
                "hash"
        );
        user.markEmailAsVerified();

        Instant now = Instant.parse("2026-10-15T12:00:00Z");
        return new SessionEntity(
                "ses_123",
                tokenHash,
                user,
                now,
                now.plusSeconds(3600)
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class TestConfiguration {

        @Bean
        SessionRepository sessionRepository() {
            return mock(SessionRepository.class);
        }

        @Bean
        CachedSessionService cachedSessionService(
                SessionRepository sessionRepository
        ) {
            return new CachedSessionService(sessionRepository);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    CachedSessionService.SESSION_CACHE
            );
        }
    }
}
