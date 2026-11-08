// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.identity.service;

import de.warp.identity.entity.UserEntity;
import de.warp.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class EmailVerificationResendService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final VerificationEmailSender verificationEmailSender;

    public EmailVerificationResendService(
            UserRepository userRepository,
            EmailVerificationService emailVerificationService,
            VerificationEmailSender verificationEmailSender
    ) {
        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository darf nicht null sein"
        );
        this.emailVerificationService = Objects.requireNonNull(
                emailVerificationService,
                "emailVerificationService darf nicht null sein"
        );
        this.verificationEmailSender = Objects.requireNonNull(
                verificationEmailSender,
                "verificationEmailSender darf nicht null sein"
        );
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email
                .trim()
                .toLowerCase(Locale.ROOT);

        Optional<UserEntity> optionalUser =
                userRepository.findByEmailIgnoreCase(
                        normalizedEmail
                ).or(() -> userRepository.findByPendingEmailIgnoreCase(
                        normalizedEmail
                ));

        if (optionalUser.isEmpty()) {
            return;
        }

        UserEntity user = optionalUser.get();
        boolean hasPendingEmailChange =
                user.getPendingEmail() != null && !user.getPendingEmail().isBlank();

        if (user.isEmailVerified() && !hasPendingEmailChange) {
            return;
        }

        String plainToken =
                emailVerificationService
                        .createVerificationToken(user);

        verificationEmailSender.sendVerificationEmail(
                user,
                plainToken
        );
    }
}