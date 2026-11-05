package de.warp.identity.service;

import de.warp.identity.exceptions.SessionNotFoundException;
import de.warp.identity.repository.SessionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

// [KI-Log Nr. KI-22] KI-generiert (Google Antigravity Gemini 3.8 Flash High) und im PR-Review optimiert (WARP-035)
@Service
public class CachedSessionService {

    public static final String SESSION_CACHE =
            "session-lookups";

    private final SessionRepository sessionRepository;

    public CachedSessionService(
            SessionRepository sessionRepository
    ) {
        this.sessionRepository = sessionRepository;
    }

    @Cacheable(
            cacheNames = SESSION_CACHE,
            sync = true
    )
    public CachedSession findByTokenHash(
            String tokenHash
    ) {
        return sessionRepository
                .findByTokenHash(tokenHash)
                .map(CachedSession::from)
                .orElseThrow(SessionNotFoundException::new);
    }

    @CacheEvict(
            cacheNames = SESSION_CACHE
    )
    public void evictByTokenHash(
            String tokenHash
    ) {
        // Die Annotation entfernt den Cache-Eintrag.
    }
}