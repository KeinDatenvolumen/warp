// [KI-Log Nr. KI-236] (201cf90) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-035): add in-memory caching
// [KI-Log Nr. KI-237] (d313c4e) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-035 improve code, implement best practises
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
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

    @CacheEvict(
            cacheNames = SESSION_CACHE,
            allEntries = true
    )
    public void evictAllSessions() {
        // Die Annotation entfernt alle gecachten Session-Lookups (z. B. nach E-Mail-Verifikation).
    }
}