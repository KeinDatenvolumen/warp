package de.warp.identity.service;

import de.warp.identity.exceptions.SessionNotFoundException;
import de.warp.identity.repository.SessionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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