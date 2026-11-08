// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.identity.service;

import de.warp.identity.entity.EmailVerificationTokenEntity;
import de.warp.identity.entity.UserEntity;
import de.warp.identity.exceptions.InvalidEmailVerificationTokenException;
import de.warp.identity.repository.EmailVerificationTokenRepository;
import de.warp.identity.repository.UserRepository;
import de.warp.identity.security.EmailVerificationTokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final Duration TOKEN_VALIDITY = Duration.ofHours(24);

    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailVerificationTokenGenerator tokenGenerator;
    private final Clock clock;
    private final UserRepository userRepository;
    private final CachedSessionService cachedSessionService;

    @Autowired
    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            EmailVerificationTokenGenerator tokenGenerator,
            @Autowired(required = false)
            UserRepository userRepository,
            @Autowired(required = false)
            CachedSessionService cachedSessionService
    ) {
        this(
                tokenRepository,
                tokenGenerator,
                Clock.systemUTC(),
                userRepository,
                cachedSessionService
        );
    }

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            EmailVerificationTokenGenerator tokenGenerator
    ) {
        this(
                tokenRepository,
                tokenGenerator,
                Clock.systemUTC(),
                null,
                null
        );
    }

    EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            EmailVerificationTokenGenerator tokenGenerator,
            Clock clock
    ) {
        this(
                tokenRepository,
                tokenGenerator,
                clock,
                null,
                null
        );
    }

    EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            EmailVerificationTokenGenerator tokenGenerator,
            Clock clock,
            UserRepository userRepository,
            CachedSessionService cachedSessionService
    ) {
        this.tokenRepository = Objects.requireNonNull(
                tokenRepository,
                "tokenRepository darf nicht null sein"
        );
        this.tokenGenerator = Objects.requireNonNull(
                tokenGenerator,
                "tokenGenerator darf nicht null sein"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock darf nicht null sein"
        );
        this.userRepository = userRepository;
        this.cachedSessionService = cachedSessionService;
    }

    @Transactional
    public String createVerificationToken(UserEntity user) {
        Objects.requireNonNull(
                user,
                "user darf nicht null sein"
        );

        Instant now = clock.instant();

        tokenRepository.markAllUnusedTokensAsUsed(
                user.getId(),
                now
        );

        var generatedToken = tokenGenerator.generate();

        var tokenEntity = new EmailVerificationTokenEntity(
                createTokenId(),
                user,
                generatedToken.tokenHash(),
                now.plus(TOKEN_VALIDITY),
                now
        );

        tokenRepository.save(tokenEntity);

        return generatedToken.plainToken();
    }

    @Transactional
    public void confirmEmail(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            throw new InvalidEmailVerificationTokenException();
        }

        String tokenHash = tokenGenerator.hash(plainToken);
        Instant now = clock.instant();

        EmailVerificationTokenEntity token = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(
                        InvalidEmailVerificationTokenException::new
                );

        if (token.isUsed() || token.isExpired(now)) {
            throw new InvalidEmailVerificationTokenException();
        }

        UserEntity user = token.getUser();
        boolean hasPendingEmailChange =
                user.getPendingEmail() != null && !user.getPendingEmail().isBlank();

        if (user.isEmailVerified() && !hasPendingEmailChange) {
            throw new InvalidEmailVerificationTokenException();
        }

        if (hasPendingEmailChange
                && userRepository != null
                && userRepository.existsByEmailIgnoreCase(user.getPendingEmail())
                && !user.getEmail().equalsIgnoreCase(user.getPendingEmail())) {
            throw new InvalidEmailVerificationTokenException();
        }

        user.markEmailAsVerified();
        token.markAsUsed(now);

        if (cachedSessionService != null) {
            cachedSessionService.evictAllSessions();
        }
    }

    private String createTokenId() {
        return "evt_" + UUID.randomUUID()
                .toString()
                .replace("-", "");
    }
}
