// [KI-Log Nr. KI-14] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Passwort-Reset-Workflow (WARP-008)
package de.warp.identity.service;

import de.warp.identity.entity.PasswordResetTokenEntity;
import de.warp.identity.entity.UserEntity;
import de.warp.identity.exceptions.InvalidPasswordResetTokenException;
import de.warp.identity.exceptions.PasswordRequirementsNotMetException;
import de.warp.identity.repository.PasswordResetTokenRepository;
import de.warp.identity.security.PasswordRequirements;
import de.warp.identity.security.PasswordResetTokenGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Duration TOKEN_VALIDITY =
            Duration.ofMinutes(30);

    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetTokenGenerator tokenGenerator;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Autowired
    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            PasswordResetTokenGenerator tokenGenerator,
            PasswordEncoder passwordEncoder
    ) {
        this(
                tokenRepository,
                tokenGenerator,
                passwordEncoder,
                Clock.systemUTC()
        );
    }

    PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            PasswordResetTokenGenerator tokenGenerator,
            PasswordEncoder passwordEncoder,
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
        this.passwordEncoder = Objects.requireNonNull(
                passwordEncoder,
                "passwordEncoder darf nicht null sein"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock darf nicht null sein"
        );
    }

    @Transactional
    public String createPasswordResetToken(UserEntity user) {
        Objects.requireNonNull(
                user,
                "user darf nicht null sein"
        );

        Instant now = clock.instant();

        tokenRepository.markAllUnusedTokensAsUsed(
                user.getId(),
                now
        );

        PasswordResetTokenGenerator.GeneratedToken generatedToken =
                tokenGenerator.generate();

        PasswordResetTokenEntity tokenEntity =
                new PasswordResetTokenEntity(
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
    public void resetPassword(
            String plainToken,
            String newPassword
    ) {
        if (plainToken == null || plainToken.isBlank()) {
            throw new InvalidPasswordResetTokenException();
        }

        String tokenHash = tokenGenerator.hash(
                plainToken
        );

        Instant now = clock.instant();

        PasswordResetTokenEntity token = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(
                        InvalidPasswordResetTokenException::new
                );

        if (token.isUsed() || token.isExpired(now)) {
            throw new InvalidPasswordResetTokenException();
        }

        if (!PasswordRequirements.areMetBy(newPassword)) {
            throw new PasswordRequirementsNotMetException();
        }

        String newPasswordHash = passwordEncoder.encode(
                newPassword
        );

        UserEntity user = token.getUser();

        user.changePasswordHash(newPasswordHash);
        token.markAsUsed(now);

        tokenRepository.markAllUnusedTokensAsUsed(
                user.getId(),
                now
        );
    }

    private String createTokenId() {
        return "prt_" + UUID.randomUUID()
                .toString()
                .replace("-", "");
    }
}