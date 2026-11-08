// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.service;

import de.warp.identity.entity.UserEntity;
import de.warp.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class PasswordResetRequestService {

    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;
    private final PasswordResetEmailSender passwordResetEmailSender;

    public PasswordResetRequestService(
            UserRepository userRepository,
            PasswordResetService passwordResetService,
            PasswordResetEmailSender passwordResetEmailSender
    ) {
        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository darf nicht null sein"
        );
        this.passwordResetService = Objects.requireNonNull(
                passwordResetService,
                "passwordResetService darf nicht null sein"
        );
        this.passwordResetEmailSender = Objects.requireNonNull(
                passwordResetEmailSender,
                "passwordResetEmailSender darf nicht null sein"
        );
    }

    @Transactional
    public void requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email
                .trim()
                .toLowerCase(Locale.ROOT);

        Optional<UserEntity> optionalUser =
                userRepository.findByEmailIgnoreCase(
                        normalizedEmail
                );

        if (optionalUser.isEmpty()) {
            return;
        }

        UserEntity user = optionalUser.get();

        String plainToken =
                passwordResetService
                        .createPasswordResetToken(user);

        passwordResetEmailSender.sendPasswordResetEmail(
                user,
                plainToken
        );
    }
}