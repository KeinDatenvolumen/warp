// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-231] (80972f5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: decouple e-mail delivery from identity service via RabbitMQ (WARP-036)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.service;

import de.warp.identity.entity.UserEntity;
import de.warp.identity.messaging.EmailRequestedEvent;
import de.warp.identity.messaging.MailType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Fordert den Versand der Passwort-Reset-E-Mail an. Die Zustellung erfolgt
 * entkoppelt nach dem Commit (siehe QueueMailEventRelay und
 * PasswordResetMailDelivery).
 */
@Service
public class PasswordResetEmailSender {

    private final ApplicationEventPublisher eventPublisher;

    public PasswordResetEmailSender(
            ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = Objects.requireNonNull(
                eventPublisher,
                "eventPublisher darf nicht null sein"
        );
    }

    public void sendPasswordResetEmail(
            UserEntity user,
            String plainToken
    ) {
        Objects.requireNonNull(user, "user darf nicht null sein");

        if (plainToken == null || plainToken.isBlank()) {
            throw new IllegalArgumentException(
                    "plainToken darf nicht leer sein"
            );
        }

        eventPublisher.publishEvent(
                EmailRequestedEvent.create(
                        MailType.PASSWORD_RESET,
                        user.getId(),
                        plainToken
                )
        );
    }
}