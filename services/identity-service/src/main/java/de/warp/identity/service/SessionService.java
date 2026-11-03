package de.warp.identity.service;

import de.warp.identity.dto.CreateSessionRequest;
import de.warp.identity.dto.CreatedSessionResponse;
import de.warp.identity.dto.SessionResponse;
import de.warp.identity.entity.SessionEntity;
import de.warp.identity.entity.UserEntity;
import de.warp.identity.exceptions.InvalidCredentialsException;
import de.warp.identity.exceptions.SessionExpiredException;
import de.warp.identity.exceptions.SessionNotFoundException;
import de.warp.identity.repository.SessionRepository;
import de.warp.identity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class SessionService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;
    private final CachedSessionService cachedSessionService;
    private final Clock clock;
    private final Duration sessionDuration;
    private final String dummyPasswordHash;

    public SessionService(
            UserRepository userRepository,
            SessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            SessionTokenService sessionTokenService,
            CachedSessionService cachedSessionService,
            Clock clock,
            @Value("${warp.session.duration:PT12H}")
            Duration sessionDuration
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionTokenService = sessionTokenService;
        this.cachedSessionService = cachedSessionService;
        this.clock = clock;
        this.sessionDuration = sessionDuration;

        this.dummyPasswordHash = passwordEncoder.encode(
                UUID.randomUUID().toString()
        );
    }

    @Transactional
    public CreatedSessionResponse createSession(
            CreateSessionRequest request
    ) {
        UserEntity user = userRepository
                .findByEmailIgnoreCase(request.email())
                .orElse(null);

        String passwordHash = user == null
                ? dummyPasswordHash
                : user.getPasswordHash();

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                passwordHash
        );

        if (user == null || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        Instant createdAt = clock.instant();
        Instant expiresAt = createdAt.plus(sessionDuration);

        String sessionToken = sessionTokenService.generateToken();
        String tokenHash = sessionTokenService.hashToken(sessionToken);

        SessionEntity session = new SessionEntity(
                generateSessionId(),
                tokenHash,
                user,
                createdAt,
                expiresAt
        );

        sessionRepository.save(session);

        return new CreatedSessionResponse(
                sessionToken,
                user.getId(),
                createdAt,
                expiresAt
        );
    }

    private String generateSessionId() {
        return "ses_" + UUID.randomUUID()
                .toString()
                .replace("-", "");
    }

    @Transactional(readOnly = true)
    public SessionResponse validateSession(String sessionToken) {
        String tokenHash = sessionTokenService.hashToken(sessionToken);

        SessionEntity session =
                cachedSessionService.findByTokenHash(tokenHash);

        if (!session.isValidAt(clock.instant())) {
            throw new SessionExpiredException();
        }

        return new SessionResponse(
                session.getId(),
                session.getUser().getId(),
                session.getCreatedAt(),
                session.getExpiresAt(),
                session.getUser().isEmailVerified()
        );
    }
    @Transactional
    public void invalidateSession(String sessionToken) {
        String tokenHash =
                sessionTokenService.hashToken(sessionToken);

        sessionRepository
                .findByTokenHash(tokenHash)
                .ifPresent(session -> {
                    session.invalidate(clock.instant());
                    sessionRepository.save(session);
                });

        cachedSessionService.evictByTokenHash(tokenHash);
    }
}