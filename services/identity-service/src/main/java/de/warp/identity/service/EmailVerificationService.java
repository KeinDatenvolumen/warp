package de.warp.identity.service;

import de.warp.identity.entity.EmailVerificationTokenEntity;
import de.warp.identity.entity.UserEntity;
import de.warp.identity.exceptions.InvalidEmailVerificationTokenException;
import de.warp.identity.repository.EmailVerificationTokenRepository;
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

    @Autowired
    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            EmailVerificationTokenGenerator tokenGenerator
    ) {
        this(
                tokenRepository,
                tokenGenerator,
                Clock.systemUTC()
        );
    }

    EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            EmailVerificationTokenGenerator tokenGenerator,
            Clock clock
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

        if (user.isEmailVerified()) {
            throw new InvalidEmailVerificationTokenException();
        }

        user.markEmailAsVerified();
        token.markAsUsed(now);
    }

    private String createTokenId() {
        return "evt_" + UUID.randomUUID()
                .toString()
                .replace("-", "");
    }
}
